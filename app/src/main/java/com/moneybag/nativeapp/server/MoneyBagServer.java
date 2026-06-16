package com.moneybag.nativeapp.server;

import android.app.Application;
import android.content.Context;
import com.google.gson.Gson;
import com.moneybag.nativeapp.data.Account;
import com.moneybag.nativeapp.data.Category;
import com.moneybag.nativeapp.data.MoneyBagRepository;
import com.moneybag.nativeapp.data.Transaction;
import fi.iki.elonen.NanoHTTPD;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class MoneyBagServer extends NanoHTTPD {

    private final Context context;
    private final MoneyBagRepository repository;
    private final Gson gson;

    public MoneyBagServer(Context context, int port) {
        super(port);
        this.context = context;
        this.repository = new MoneyBagRepository((Application) context.getApplicationContext());
        this.gson = new Gson();
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        Method method = session.getMethod();

        try {
            if (uri.startsWith("/api/")) {
                return handleApiRequest(uri, method, session);
            } else {
                return serveStaticFiles(uri);
            }
        } catch (Exception e) {
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Internal Error: " + e.getMessage());
        }
    }

    private Response handleApiRequest(String uri, Method method, IHTTPSession session) {
        if (method == Method.GET) {
            if (uri.equals("/api/accounts")) {
                return handleGetAccounts();
            } else if (uri.equals("/api/transactions")) {
                return handleGetTransactions();
            } else if (uri.equals("/api/categories")) {
                return handleGetCategories();
            }
        } else if (method == Method.POST) {
            if (uri.equals("/api/transactions")) {
                return handlePostTransaction(session);
            } else if (uri.equals("/api/accounts")) {
                return handlePostAccount(session);
            } else if (uri.equals("/api/categories")) {
                return handlePostCategory(session);
            }
        }
        
        return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "API Endpoint not found");
    }

    private Response handlePostTransaction(IHTTPSession session) {
        try {
            Map<String, String> files = new HashMap<>();
            session.parseBody(files);
            String jsonBody = files.get("postData");
            
            // If postData is null, it might be in the stream
            if (jsonBody == null) {
                // Not ideal for large bodies but fine for simple transactions
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Missing body");
            }

            Transaction transaction = gson.fromJson(jsonBody, Transaction.class);
            repository.insertTransaction(transaction);
            return createJsonResponse("{\"status\":\"success\"}");
        } catch (Exception e) {
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, e.getMessage());
        }
    }

    private Response handlePostAccount(IHTTPSession session) {
        try {
            Map<String, String> files = new HashMap<>();
            session.parseBody(files);
            String jsonBody = files.get("postData");
            if (jsonBody == null) return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Missing body");

            Account account = gson.fromJson(jsonBody, Account.class);
            repository.insertAccount(account);
            return createJsonResponse("{\"status\":\"success\"}");
        } catch (Exception e) {
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, e.getMessage());
        }
    }

    private Response handlePostCategory(IHTTPSession session) {
        try {
            Map<String, String> files = new HashMap<>();
            session.parseBody(files);
            String jsonBody = files.get("postData");
            if (jsonBody == null) return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Missing body");

            Category category = gson.fromJson(jsonBody, Category.class);
            repository.insertCategory(category);
            return createJsonResponse("{\"status\":\"success\"}");
        } catch (Exception e) {
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, e.getMessage());
        }
    }

    private Response handleGetAccounts() {
        AtomicReference<String> json = new AtomicReference<>();
        repository.getAllAccounts(accounts -> {
            json.set(gson.toJson(accounts));
            synchronized (json) { json.notify(); }
        });
        
        synchronized (json) {
            try { if (json.get() == null) json.wait(5000); } catch (InterruptedException e) {}
        }
        
        return createJsonResponse(json.get());
    }

    private Response handleGetTransactions() {
        AtomicReference<String> json = new AtomicReference<>();
        repository.getAllTransactions(transactions -> {
            json.set(gson.toJson(transactions));
            synchronized (json) { json.notify(); }
        });
        
        synchronized (json) {
            try { if (json.get() == null) json.wait(5000); } catch (InterruptedException e) {}
        }
        
        return createJsonResponse(json.get());
    }

    private Response handleGetCategories() {
        AtomicReference<String> json = new AtomicReference<>();
        repository.getAllCategories(categories -> {
            json.set(gson.toJson(categories));
            synchronized (json) { json.notify(); }
        });
        
        synchronized (json) {
            try { if (json.get() == null) json.wait(5000); } catch (InterruptedException e) {}
        }
        
        return createJsonResponse(json.get());
    }

    private Response createJsonResponse(String json) {
        if (json == null) return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json; charset=utf-8", "{}");
        Response response = newFixedLengthResponse(Response.Status.OK, "application/json; charset=utf-8", json);
        response.addHeader("Access-Control-Allow-Origin", "*");
        return response;
    }

    private Response serveStaticFiles(String uri) {
        if (uri.equals("/") || uri.isEmpty()) {
            uri = "/index.html";
        }

        String path = "www" + uri;
        try {
            java.io.InputStream is = context.getAssets().open(path);
            String mimeType = getMimeTypeForAsset(uri);
            return newChunkedResponse(Response.Status.OK, mimeType, is);
        } catch (IOException e) {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "File not found: " + path);
        }
    }

    private String getMimeTypeForAsset(String uri) {
        if (uri.endsWith(".html")) return "text/html";
        if (uri.endsWith(".js")) return "application/javascript";
        if (uri.endsWith(".css")) return "text/css";
        if (uri.endsWith(".png")) return "image/png";
        if (uri.endsWith(".jpg") || uri.endsWith(".jpeg")) return "image/jpeg";
        return MIME_PLAINTEXT;
    }
}
