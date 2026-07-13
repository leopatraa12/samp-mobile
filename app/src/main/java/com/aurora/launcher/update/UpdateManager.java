package com.aurora.launcher.update;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class UpdateManager {

    private Context context;

    public UpdateManager(Context context) {
        this.context = context;
    }

    public File getGameDir() {
        return new File(
                Environment.getExternalStorageDirectory(),
                "Android/data/com.aurora.game/files"
        );
    }

    public void startUpdate(String jsonUrl) {

        new Thread(() -> {
            try {

                String json = load(jsonUrl);
                JSONObject obj = new JSONObject(json);

                JSONObject hf = obj.getJSONObject("huggingface");

                String repoId = hf.getString("repo_id");
                String repoType = hf.optString("repo_type", "dataset");
                String revision = hf.optString("revision", "main");
                String prefix = hf.optString("files_path_prefix", "");

                String repoTypePath = "datasets/";
                if ("space".equalsIgnoreCase(repoType) || "spaces".equalsIgnoreCase(repoType)) {
                    repoTypePath = "spaces/";
                } else if ("model".equalsIgnoreCase(repoType) || "models".equalsIgnoreCase(repoType)) {
                    repoTypePath = "models/";
                }

                String treeUrl = "https://huggingface.co/api/" + repoTypePath + repoId + "/tree/" + revision + "?recursive=true&expand=true";
                String resolveBaseUrl = "https://huggingface.co/" + repoTypePath + repoId + "/resolve/" + revision;

                ArrayList<JSONObject> fileEntries = new ArrayList<>();
                Set<String> remotePaths = new HashSet<>();
                String nextUrl = treeUrl;

                while (nextUrl != null && !nextUrl.trim().isEmpty()) {
                    HttpURLConnection connection = null;
                    BufferedReader reader = null;
                    try {
                        connection = (HttpURLConnection) new URL(nextUrl).openConnection();
                        connection.setConnectTimeout(30000);
                        connection.setReadTimeout(30000);
                        connection.connect();

                        int responseCode = connection.getResponseCode();
                        if (responseCode < 200 || responseCode >= 300) {
                            throw new IOException("Unexpected Hugging Face response code: " + responseCode);
                        }

                        reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        StringBuilder buffer = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            buffer.append(line);
                        }

                        JSONArray entries = new JSONArray(buffer.toString());
                        for (int i = 0; i < entries.length(); i++) {
                            JSONObject item = entries.optJSONObject(i);
                            if (item == null || !"file".equalsIgnoreCase(item.optString("type"))) {
                                continue;
                            }

                            String remotePath = item.optString("path", "").trim();
                            if (remotePath.isEmpty()) {
                                continue;
                            }

                            fileEntries.add(item);
                            remotePaths.add(remotePath);
                        }

                        nextUrl = parseNextLink(connection.getHeaderField("Link"));
                    } finally {
                        if (reader != null) {
                            try {
                                reader.close();
                            } catch (IOException ignored) {}
                        }
                        if (connection != null) {
                            connection.disconnect();
                        }
                    }
                }

                for (JSONObject entry : fileEntries) {
                    String remotePath = entry.optString("path", "").trim();
                    if (remotePath.isEmpty() || remotePath.startsWith(".")) {
                        continue;
                    }

                    String localPath = resolveHuggingFaceLocalPath(remotePath, prefix, remotePaths);
                    if (localPath.isEmpty()) {
                        continue;
                    }

                    String downloadUrl = joinUrl(resolveBaseUrl, remotePath);
                    download(downloadUrl, localPath);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private String parseNextLink(String linkHeader) {
        if (linkHeader == null || linkHeader.trim().isEmpty()) {
            return "";
        }

        String[] parts = linkHeader.split(",");
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.contains("rel=\"next\"")) {
                continue;
            }

            int start = trimmed.indexOf('<');
            int end = trimmed.indexOf('>');
            if (start >= 0 && end > start) {
                return trimmed.substring(start + 1, end);
            }
        }

        return "";
    }

    private String resolveHuggingFaceLocalPath(String remotePath, String prefix, Set<String> remotePaths) {
        String normalizedPrefix = prefix == null ? "" : prefix.trim();
        if (!normalizedPrefix.isEmpty()) {
            if (remotePath.equals(normalizedPrefix)) {
                return "";
            }

            String expectedPrefix = normalizedPrefix + "/";
            if (!remotePath.startsWith(expectedPrefix)) {
                return "";
            }

            return remotePath.substring(expectedPrefix.length());
        }

        if (remotePath.startsWith("files/")) {
            String strippedPath = remotePath.substring("files/".length());
            if (remotePaths.contains(strippedPath)) {
                return "";
            }
            return strippedPath;
        }

        return remotePath;
    }

    private String joinUrl(String baseUrl, String relativePath) {
        String sanitizedBaseUrl = baseUrl == null ? "" : baseUrl.trim();
        if (sanitizedBaseUrl.isEmpty()) {
            return "";
        }
        if (!sanitizedBaseUrl.endsWith("/")) {
            sanitizedBaseUrl = sanitizedBaseUrl + "/";
        }
        return sanitizedBaseUrl + relativePath;
    }

    private String load(String url) throws Exception {
        BufferedReader br = new BufferedReader(
                new InputStreamReader(new URL(url).openStream())
        );

        StringBuilder sb = new StringBuilder();
        String line;

        while ((line = br.readLine()) != null) {
            sb.append(line);
        }

        return sb.toString();
    }

    private void download(String url, String name) {
        try {
            File out = new File(getGameDir(), name);
            File parent = out.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }

            InputStream in = new URL(url).openStream();
            FileOutputStream fos = new FileOutputStream(out);

            byte[] buf = new byte[8192];
            int len;

            while ((len = in.read(buf)) != -1) {
                fos.write(buf, 0, len);
            }

            fos.close();
            in.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}