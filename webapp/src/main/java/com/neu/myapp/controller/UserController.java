
package com.neu.myapp.controller;


import com.neu.myapp.repository.UserRepository;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

import com.timgroup.statsd.StatsDClient;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import com.google.gson.JsonObject;
import com.neu.myapp.model.User;
import com.google.gson.Gson;
import org.json.simple.JSONObject;
import org.springframework.security.crypto.bcrypt.BCrypt;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSAsyncClientBuilder;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.Topic;
import java.util.Date;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
@RestController
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StatsDClient statsDClient;

    private final static Logger logger= LoggerFactory.getLogger(UserController.class);
    @RequestMapping(value = "/", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ResponseEntity<String> welcome() {
        statsDClient.incrementCounter("_UserLoggedIn_API_");
        logger.info("Inside__UserLoggedIn_API_");
        JsonObject jsonObject = new JsonObject();

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentPrincipalName = authentication.getName();

        if(currentPrincipalName.equals("Unauthorized")){
            jsonObject.addProperty("message", "You are not logged in");
            logger.error("Unauthorized");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(jsonObject.toString());
        }
        else if(currentPrincipalName.equals("BadRequest")){
            jsonObject.addProperty("message", "Please enter valid credentials");
            logger.error("Bad request");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(jsonObject.toString());
        }
        else{
            jsonObject.addProperty("message", "you are logged in. current time is " + new Date().toString());
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(jsonObject.toString());
        }
    }

    @RequestMapping(value = "/user/register", method = RequestMethod.POST, produces = "application/json")
    @ResponseBody
    public ResponseEntity<String> registerPost(@RequestBody String sUser) {
        statsDClient.incrementCounter("_UserRegister_API_");
        logger.info("Inside_UserRegisterinStatusCheck_API_");
        Gson gson = new Gson();
        User user = gson.fromJson(sUser, User.class);
        JsonObject jsonObject = new JsonObject();

        String email = user.getEmail();

        final Pattern VALID_EMAIL_ADDRESS_REGEX =
                Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);
        final Pattern VALID_PASSWORD_REGEX =
                Pattern.compile("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$", Pattern.MULTILINE);

        if(user.getEmail()==null){
            jsonObject.addProperty("invalidEmail", "Please Enter Email");
            logger.error("Enter valid email");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(jsonObject.toString());
        }
        else if(user.getPassword()==null){
            jsonObject.addProperty("invalidEmail", "Please Enter Password");
            logger.error("Enter valid Password");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(jsonObject.toString());
        }
        if (!VALID_EMAIL_ADDRESS_REGEX.matcher(email).find()) {
            jsonObject.addProperty("invalidEmail", "Please provide valid Email!");
            logger.error("Please provide valid Email!");
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(jsonObject.toString());

        } else if (!VALID_PASSWORD_REGEX.matcher(user.getPassword()).find()) {
            jsonObject.addProperty("invalidPassword", "Please provide valid Password!");
            logger.error("Please provide valid Password!");
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(jsonObject.toString());
        }

        String pw_hash = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt());
        user.setPassword(pw_hash);

        User user_db = userRepository.findByEmail(user.getEmail());

        if (user_db == null) {
            userRepository.save(user);
            jsonObject.addProperty("message", "Hi " + user.getName() + ", register successfully! ");
            return ResponseEntity.status(HttpStatus.CREATED).body(jsonObject.toString());
        } else {
            jsonObject.addProperty("message", "Register failure!  " + user.getName() + " already exists! ");
            logger.error("Register failure");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(jsonObject.toString());
        }
    }


    @RequestMapping(value="/reset" , method=RequestMethod.POST)
    public ResponseEntity<String> resetPassword(@RequestBody  String email, HttpServletRequest request, HttpServletResponse response){

        statsDClient.incrementCounter("_ResetPassword_API_");
        logger.info("Inside_ResetPassword_API_");
        String parseEmail = "";
        JSONParser parser = new JSONParser();
        try {
            JSONObject jo = (JSONObject) parser.parse(email);
            parseEmail = (String)jo.get("email");
            logger.info("JSON parsed email: " + parseEmail);

        }
        catch(ParseException ex){
            logger.error("Error parsing email JSON for reset password" + ex.toString());
        }
        JsonObject jsonObject = new JsonObject();
        User up =  userRepository.findByEmail(parseEmail);

        if(up != null)
        {
            AmazonSNS snsClient = AmazonSNSAsyncClientBuilder.standard()
                    .withCredentials(new InstanceProfileCredentialsProvider(false))
                    .build();
            List<Topic> topics = snsClient.listTopics().getTopics();

            for(Topic topic: topics)
            {

                if(topic.getTopicArn().endsWith("password_reset")){
                    System.out.print(up.getEmail());
                    PublishRequest req = new PublishRequest(topic.getTopicArn(),up.getEmail());
                    snsClient.publish(req);
                    break;
                }
            }
            return ResponseEntity.status(HttpStatus.CREATED).body("");

        }
        else{
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("");
        }
    }

}