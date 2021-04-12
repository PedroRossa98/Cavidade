package com.elab.webcomm.controller;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller()
// @PropertySource(ignoreResourceNotFound = true, value = "classpath:git.properties")
public class ElabController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

//    @Autowired
//    BuildProperties buildProperties;
//
//    @Autowired
//    private Environment env;
    
    
    @RequestMapping(path = "/")
    @ResponseBody 
    public String sayHello() {
        logger.info("GET called on /hello resource");
        return "Hello from Spring Boot E-Lab Controller (not static) !";
    }
    

//    @PostMapping(value = "/getenv")
//    @ResponseBody 
//    public String getEnvData(HttpServletRequest request,
//                             HttpServletResponse response,
//                             @RequestBody Map<String, String> userData) {
//
//        logger.debug("E-Lab Controller getEnvData userData : " + userData);
//        String env = userData.get("env");
//        String var = String.valueOf(System.getenv(env));
//        
//        // logger.debug("E-Lab Controller getEnvData : " + var);
//         return var;
//    }
    
    
//    @RequestMapping(path = "/info")
//    @ResponseBody
//    public String info() {
//        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//        
//        String branch = env.getProperty("git.branch");
//        String time = env.getProperty("git.commit.time");
//        String msg = env.getProperty("git.commit.message.full");
//        
//        if (time != null) {
//            time = time.replace("T",  " ").split("\\+")[0];
//        }
//        
//        logger.info("GET called on /info resource");
//        logger.info("getName " + String.valueOf(buildProperties.getName()));
//        logger.info("getTime " + String.valueOf(buildProperties.getTime()));
//        logger.info("getVersion " + String.valueOf(buildProperties.getVersion()));
//        logger.info("artifactId " + String.valueOf(buildProperties.getArtifact()));
//        logger.info("java.source " + String.valueOf(buildProperties.get("java.source")));
//        logger.info("java.target " + String.valueOf(buildProperties.get("java.target")));
//        logger.info("build.version " + String.valueOf(buildProperties.get("build.version")));
//        logger.info("git.branch " + String.valueOf(buildProperties.get("git.branch")));
//        
//
//        String[] infoDataSourceList = {}; // TODO : 
//        String infoDataSource = "";
//        if (infoDataSourceList.length > 0) {
//            infoDataSource = infoDataSourceList[1];
//        }
//
//        if (infoDataSourceList.length > 2) {
//            if (infoDataSourceList[3].split("/").length > 0) {
//                infoDataSource += " - " + infoDataSourceList[3].split("/")[1];
//            }
//        }
//
//        String build = "Unkown";
//        if  (branch != null && time != null && msg !=null) {
//            build = branch + " - " + time + " - " + msg;
//        }
//        
//        String strTime = " (" + sdf.format(buildProperties.getTime().toEpochMilli()) + ")";
//        String tag = "  (build " + build + ")";
//        String message = "<div align=\"left\" style=\"margin-left: 180px;\">" + "<b> Server :</b><br/>"
//                + "<div align=\"left\" style=\"margin-left: 20px;\">" + "v."
//                + String.valueOf(buildProperties.get("build.version")) + strTime + " - java "
//                + String.valueOf(buildProperties.get("java.source")) + tag + "<div>" + "<div><br/>"
//                + "<div align=\"left\" style=\"margin-left: -20px;\">" + "<b>Database :</b><br/>"
//                + "<div align=\"left\" style=\"margin-left: 20px;\">" + infoDataSource + "<div>" + "<div>";
//        return message;
//    }

    
    @GetMapping(value="/registration")
    public String register(){
        return "register";
    }

    @GetMapping(value="/login")
    public String login(){
        return "login";
    }

}
