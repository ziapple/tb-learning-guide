package com.ziapple.test;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.ComponentScan;

@SpringBootConfiguration
@ComponentScan({"com.ziapple.server"})
public class AkkaClusterServer2 {
    public static String[] updateArguments(String[] args) {
            String[] modifiedArgs = new String[args.length + 2];
            System.arraycopy(args, 0, modifiedArgs, 0, args.length);
            modifiedArgs[args.length] = "--rpc.bind_host=127.0.0.1";
            modifiedArgs[args.length + 1] = "--rpc.bind_port=8080";
            return modifiedArgs;
    }
}
