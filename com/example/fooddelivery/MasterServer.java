package com.example.fooddelivery;

import com.example.fooddelivery.model.Product;
import com.example.fooddelivery.model.Store;

import java.io.*;
import java.net.*;
import java.util.*;

public class MasterServer {

    private static List<WorkerInfo> workers = new ArrayList<>();
    private static Map<UUID, ObjectOutputStream> pendingOrders = new HashMap<>();
    private static Map<UUID, Map<String, Object>> bufferedResponses = new HashMap<>();

    public static void main(String[] args) {
        if (args.length < 2 || args.length % 2 != 1) {
            System.out.println("Usage: java MasterServer <port> <workerHost1> <workerPort1> <workerHost2> <workerPort2> ...");
            return;
        }

        int masterPort = Integer.parseInt(args[0]);

        for (int i = 1; i < args.length; i += 2) {
            String host = args[i];
            int port = Integer.parseInt(args[i + 1]);
            workers.add(new WorkerInfo(host, port));
        }

        try (ServerSocket serverSocket = new ServerSocket(masterPort)) {
            System.out.println("Master listening on port " + masterPort);

            while (true) {
                Socket incomingSocket = serverSocket.accept();
                System.out.println("Connected: " + incomingSocket.getInetAddress().getHostAddress());
                new Thread(new ConnectionHandler(incomingSocket)).start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ConnectionHandler implements Runnable {
        private Socket socket;

        public ConnectionHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (
                ObjectOutputStream outToClient = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream inFromClient = new ObjectInputStream(socket.getInputStream())
            ) {
                Object firstObject = inFromClient.readObject();

                if (firstObject instanceof Store) {
                    // Manager case
                    Store store = (Store) firstObject;
                    String storeName = store.getStoreName();

                    int workerIndex = Math.abs(storeName.hashCode()) % workers.size();
                    WorkerInfo worker = workers.get(workerIndex);

                    try (
                        Socket workerSocket = new Socket(worker.host, worker.port);
                        ObjectOutputStream outToWorker = new ObjectOutputStream(workerSocket.getOutputStream());
                        ObjectInputStream inFromWorker = new ObjectInputStream(workerSocket.getInputStream());
                    ) {
                        outToWorker.flush();
                        outToWorker.writeObject(store);
                        outToWorker.flush();

                        System.out.println("Store sent to Worker " + workerIndex);
                    }


                    outToClient.writeObject("Store sent to Worker " + workerIndex);
                    outToClient.flush();

                } else if (firstObject instanceof Map) {
                    // Client case
                    Map<String, Object> request = (Map<String, Object>) firstObject;

                     if ("SEARCH".equals(request.get("type"))) {
                        UUID requestId = UUID.randomUUID();
                        request.put("requestId", requestId);

                        
                        for (WorkerInfo worker : workers) {
                            try (
                                Socket workerSocket = new Socket(worker.host, worker.port);
                                ObjectOutputStream outToWorker = new ObjectOutputStream(workerSocket.getOutputStream());
                                ObjectInputStream inFromWorker = new ObjectInputStream(workerSocket.getInputStream())
                            ) {
                                outToWorker.writeObject(request);
                                outToWorker.flush();
                                System.out.println("[Master] Sent SEARCH request with ID " + requestId + " to Worker " + worker.port);

                            } catch (Exception e) {
                                System.out.println("[Master] Failed to send request to Worker " + worker.port);
                                e.printStackTrace();
                            }
                        }

                        
                        List<Store> finalResult = new ArrayList<>();
                        try (
                            Socket reducerSocket = new Socket("127.0.0.1", 7000);
                            ObjectOutputStream reducerOut = new ObjectOutputStream(reducerSocket.getOutputStream());
                            ObjectInputStream reducerIn = new ObjectInputStream(reducerSocket.getInputStream())
                        ) {
                            
                            reducerOut.writeObject(requestId);
                            reducerOut.flush();

                            
                            Object result = reducerIn.readObject();
                            if (result instanceof List<?>) {
                                for (Object obj : (List<?>) result) {
                                    if (obj instanceof Store) {
                                        finalResult.add((Store) obj);
                                    }
                                }
                                System.out.println("[Master] Received final result from Reducer: " + finalResult.size() + " stores.");
                            } else {
                                System.out.println("[Master] Reducer responded with unexpected data.");
                            }

                            
                            Thread.sleep(100);

                        } catch (Exception e) {
                            System.out.println("[Master] Error communicating with Reducer");
                            e.printStackTrace();
                        }

                        
                        try {
                            outToClient.writeObject(finalResult);
                            outToClient.flush();
                        } catch (IOException e) {
                            System.out.println("[Master] Failed to send result to Client");
                            e.printStackTrace();
                        }
                    }

                    else if ("ORDER".equals(request.get("type"))) {
                        String storeName = (String) request.get("storeName");
                        UUID orderId = (UUID) request.get("orderId");
                        pendingOrders.put(orderId, outToClient);

                        if (bufferedResponses.containsKey(orderId)) {
                            Map<String, Object> existingResponse = bufferedResponses.remove(orderId);
                            outToClient.writeObject(existingResponse);
                            outToClient.flush();
                            pendingOrders.remove(orderId);
                            return;
                        }

                        int workerIndex = Math.abs(storeName.hashCode()) % workers.size();
                        WorkerInfo worker = workers.get(workerIndex);

                        try (
                            Socket workerSocket = new Socket(worker.host, worker.port);
                            ObjectOutputStream outToWorker = new ObjectOutputStream(workerSocket.getOutputStream());
                            ObjectInputStream inFromWorker = new ObjectInputStream(workerSocket.getInputStream())
                        ) {
                            outToWorker.writeObject(request);
                            outToWorker.flush();

                            while (true) {
                                Map<String, Object> workerResponse = (Map<String, Object>) inFromWorker.readObject();
                                UUID orderId2 = (UUID) workerResponse.get("orderId");

                                if (orderId2 == null) continue;

                                if (orderId2 != null && orderId2.equals(orderId)) {
                                    outToClient.writeObject(workerResponse);
                                    outToClient.flush();
                                    pendingOrders.remove(orderId); 
                                    System.out.println("Order forwarded to Worker " + workerIndex);
                                    break;
                                }
                                else {
                                    bufferedResponses.put(orderId2, workerResponse);
                                    System.out.println("Received unrelated orderId " + orderId2 + " - buffered for later.");
                                }
                            }

                        } catch (Exception e) {
                            outToClient.writeObject("Error sending order to worker.");
                            outToClient.flush();
                            e.printStackTrace();
                        }

                    }else if ("RATING".equals(request.get("type"))) {
                        String storeName = (String) request.get("storeName");
                        int workerIndex = Math.abs(storeName.hashCode()) % workers.size();
                        WorkerInfo worker = workers.get(workerIndex);
                    
                        try (
                            Socket workerSocket = new Socket(worker.host, worker.port);
                            ObjectOutputStream outToWorker = new ObjectOutputStream(workerSocket.getOutputStream());
                            ObjectInputStream inFromWorker = new ObjectInputStream(workerSocket.getInputStream())
                        ) {
                            outToWorker.writeObject(request);
                            outToWorker.flush();
                    
                            Object workerResponse = inFromWorker.readObject();
                            outToClient.writeObject(workerResponse);
                            outToClient.flush();
                    
                            System.out.println("Rating forwarded to Worker " + workerIndex);
                        } catch (Exception e) {
                            outToClient.writeObject("Error sending rating to worker.");
                            outToClient.flush();
                            e.printStackTrace();
                        }
                    } 
                    
                    else if ("GET_ALL_STORES".equals(request.get("type"))) {
                        List<Store> allStores = new ArrayList<>();
                    
                        for (WorkerInfo worker : workers) {
                            try (
                                Socket workerSocket = new Socket(worker.host, worker.port);
                                ObjectOutputStream outToWorker = new ObjectOutputStream(workerSocket.getOutputStream());
                                ObjectInputStream inFromWorker = new ObjectInputStream(workerSocket.getInputStream())
                            ) {
                                Map<String, Object> getStoresRequest = new HashMap<>();
                                getStoresRequest.put("type", "GET_STORES");
                    
                                outToWorker.writeObject(getStoresRequest);
                                outToWorker.flush();
                    
                                Object response = inFromWorker.readObject();
                                if (response instanceof List<?>) {
                                    allStores.addAll((List<Store>) response);
                                }
                    
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    
                        outToClient.writeObject(allStores);
                        outToClient.flush();
                    }
                    

                }
                 else {
                    outToClient.writeObject("Unknown object received.");
                    outToClient.flush();
                }

            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private static class WorkerInfo {
        String host;
        int port;

        WorkerInfo(String host, int port) {
            this.host = host;
            this.port = port;
        }
    }
}