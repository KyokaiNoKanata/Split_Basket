package com.example.split_basket;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class InventoryRepository {
    private static final String PREF = "inventory_repo";
    private static final String KEY_ITEMS = "items";
    private static final String KEY_LOGS = "logs";

    private final SharedPreferences prefs;

    public InventoryRepository(Context ctx) {
        this.prefs = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public synchronized List<InventoryItem> getItems() {
        String raw = prefs.getString(KEY_ITEMS, "[]");
        List<InventoryItem> list = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                list.add(InventoryItem.fromJson(arr.getJSONObject(i)));
            }
        } catch (JSONException ignored) {}
        return list;
    }

    private synchronized void saveItems(List<InventoryItem> items) {
        JSONArray arr = new JSONArray();
        for (InventoryItem it : items) {
            try { arr.put(it.toJson()); } catch (JSONException ignored) {}
        }
        prefs.edit().putString(KEY_ITEMS, arr.toString()).apply();
    }

    public synchronized void addItem(InventoryItem it) {
        List<InventoryItem> items = getItems();
        items.add(it);
        saveItems(items);
        appendLog("IN", it);
    }

    public synchronized void removeItem(String id) {
        List<InventoryItem> items = getItems();
        InventoryItem target = null;
        for (InventoryItem it : items) {
            if (it.id.equals(id)) { target = it; break; }
        }
        if (target != null) {
            items.remove(target);
            saveItems(items);
            appendLog("OUT", target);
        }
    }

    public synchronized void updateItem(InventoryItem updated) {
        List<InventoryItem> items = getItems();
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).id.equals(updated.id)) {
                items.set(i, updated);
                saveItems(items);
                appendLog("UPDATE", updated);
                return;
            }
        }
    }

    public synchronized List<String> getLogs() {
        String raw = prefs.getString(KEY_LOGS, "[]");
        List<String> out = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) out.add(arr.getString(i));
        } catch (JSONException ignored) {}
        return out;
    }

    private synchronized void appendLog(String type, InventoryItem it) {
        String raw = prefs.getString(KEY_LOGS, "[]");
        JSONArray arr;
        try { arr = new JSONArray(raw); } catch (JSONException e) { arr = new JSONArray(); }
        String line = System.currentTimeMillis() + " | " + type + " | " + it.name + " x" + it.quantity + " | " + it.category;
        arr.put(line);
        prefs.edit().putString(KEY_LOGS, arr.toString()).apply();
    }
}