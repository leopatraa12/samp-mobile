package com.aurora.launcher.update;

public class UpdateInfo {

    public String version;
    public HuggingFace huggingface;

    public static class HuggingFace {
        public String repo_id;
        public String repo_type;
        public String revision;
        public String client_config_path;
        public String files_path_prefix;
    }
}