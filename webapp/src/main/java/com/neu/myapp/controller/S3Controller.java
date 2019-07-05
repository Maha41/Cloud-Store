package com.neu.myapp.controller;


import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.neu.myapp.model.Attachment;
import com.neu.myapp.model.Note;
import com.neu.myapp.model.User;
import com.neu.myapp.repository.AttachmentRepository;
import com.neu.myapp.repository.NoteRepository;
import com.neu.myapp.repository.UserRepository;
import com.timgroup.statsd.StatsDClient;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@SuppressWarnings("ALL")
@Profile("aws")
@RestController
public class S3Controller {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private NoteRepository noteRepository;
    @Autowired
    private AttachmentRepository attachmentRepository;

    @Autowired
    private StatsDClient statsDClient;

    private final static Logger logger= LoggerFactory.getLogger(UserController.class);

    String clientRegion = "us-east-1";

    AmazonS3 s3client = AmazonS3ClientBuilder.standard()
            .withRegion(clientRegion)
            .withCredentials(new InstanceProfileCredentialsProvider(false))
            .build();
    @Value("${endpointUrl}")
    private String endpointUrl;

    @Value("${profileName}")
    private String profile;
    @Value("${bucketName}")
    private String bucketName;

    private String BasicAuth() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentPrincipalName = authentication.getName();

        if (currentPrincipalName.equals("Unauthorized")) {
            return "unauthorized";
        } else if (currentPrincipalName.equals("BadRequest")) {
            return "BadRequest";
        } else
            return "authorized";
    }

    @RequestMapping(value = "/note/{id}/attachments", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<String> attachS3File(@PathVariable String id, @RequestParam("file") MultipartFile file, Principal principal, HttpServletRequest req, HttpServletResponse response) throws Exception {
        statsDClient.incrementCounter("_CreateAttachment_API_");
        logger.info("Inside__CreateAttachment_API_");
        JsonObject jsonObject = new JsonObject();
        if (BasicAuth().equals("unauthorized")) {
            jsonObject.addProperty("message", "You are not logged in!!");
            logger.error("Bad request");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(jsonObject.toString());
        } else if (BasicAuth().equals("BadRequest")) {
            jsonObject.addProperty("message", "Please enter valid credentials!");
            logger.error("Bad request");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(jsonObject.toString());
        } else {
            //AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
            response.setContentType(ContentType.APPLICATION_JSON.getMimeType());
            User user;
            Note note;
            note = noteRepository.findById(UUID.fromString(id));
            user = userRepository.findByEmail(principal.getName());
            if (note == null) {
                jsonObject.addProperty("message", "Note not found!");
                logger.error("Note not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(jsonObject.toString());
            }
            else if(user != note.getUser()){
                jsonObject.addProperty("message", "Unauthorized to access this note!");
                logger.error("Unauthorized");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(jsonObject.toString());
            } else {
                //String folder = "/myFile";
                //String relativePath = System.getProperty("user.dir");
                if(file != null) {
                    //String filePath = saveFile(file, "" + relativePath);
                    Attachment attachment = new Attachment();
                    //attachment.setPath(filePath);
                    attachment.setNote(note);
                    attachmentRepository.save(attachment);
                    try {
                        //String keyName = note.getId() + ":" + attachment.getId().toString();
                        //File fileToUpload;
                        //fileToUpload = transferFile(file, "" + relativePath);


                        //PutObjectRequest p = new PutObjectRequest(this.bucketName , keyName , file.getInputStream(), new ObjectMetadata());
                        //p.withCannedAcl(CannedAccessControlList.PublicRead);
                        //s3client.putObject(p);

                        //new PutObjectResult();

                        String uploadDir = "/uploads/";
                        String realPath2Upload = req.getServletContext().getRealPath(uploadDir);
                        if(! new File(realPath2Upload).exists())
                        {
                            new File(realPath2Upload).mkdir();
                        }

                        String filePath2Upload = realPath2Upload+file.getOriginalFilename();
                        //String keyName = file.getOriginalFilename();
                        String keyName = note.getId() + ":" + attachment.getId().toString();
                        File saveFile = new File(filePath2Upload);
                        //System.out.println(file+".....................................FILE CHECK");
                        file.transferTo(saveFile);

                        ObjectMetadata metadata = new ObjectMetadata();
                        metadata.setContentLength(file.getSize());
                        InputStream inputStream = new FileInputStream(saveFile);
                        s3client.putObject(new PutObjectRequest(bucketName, keyName, inputStream, metadata).withCannedAcl(CannedAccessControlList.PublicRead));




                        //System.out.println(file.getInputStream()+".....................................FILE CHECK");
                        //s3client = new AmazonS3Client(credentials);
                        //bucketName = s3client.listBuckets().get(0).getName();
                        //s3client.putObject(new PutObjectRequest(bucketName, keyName, fileToUpload).withCannedAcl(CannedAccessControlList.PublicRead));
                        String filename = s3client.getUrl(bucketName, keyName).toString().substring(s3client.getUrl(bucketName, keyName).toString().lastIndexOf("/") + 1);
                        String fileUrl = endpointUrl + "/" + bucketName + "/" + filename;

                        attachment.setPath(fileUrl);
                        attachmentRepository.save(attachment);

                    } catch (AmazonServiceException ase) {
                        jsonObject.addProperty("Status", "Caught an AmazonServiceException, which " +
                                "means your request made it " +
                                "to Amazon S3, but was rejected with an error response" +
                                " for some reason.");
                        jsonObject.addProperty("Error Message    ", ase.getMessage());
                        jsonObject.addProperty("HTTP Status Code ", ase.getStatusCode());
                        jsonObject.addProperty("AWS Error Code   ", ase.getErrorCode());
                        jsonObject.addProperty("Error Type       ", ase.getErrorType().toString());
                        jsonObject.addProperty("Request ID       ", ase.getRequestId());
                        logger.error("Bad request");
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(jsonObject.toString());

                    } catch (AmazonClientException ace) {
                        jsonObject.addProperty("Status", "Caught an AmazonClientException, which " +
                                "means the client encountered " +
                                "an internal error while trying to " +
                                "communicate with S3, " +
                                "such as not being able to access the network.");
                        jsonObject.addProperty("Error Message: ", ace.getMessage());
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(jsonObject.toString());
                    }
                    jsonObject.addProperty("attachment_id", attachment.getId().toString());
                    jsonObject.addProperty("url", attachment.getPath());

                    logger.error("Bad request");
                    return ResponseEntity.status(HttpStatus.OK).body(jsonObject.toString());
                }
                else{
                    jsonObject.addProperty("message", "Please select file to Attach!");
                    logger.error("Bad request");
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(jsonObject.toString());
                }
            }
        }
    }

    //save file
    private String saveFile(MultipartFile file, String path) throws IOException {

        if(!file.isEmpty()) {
            String filename = file.getOriginalFilename();
            File filepath = new File(path,filename);

            //if path not exist, create the folder
            if (!filepath.getParentFile().exists()) {
                filepath.getParentFile().mkdirs();
            }
            String finalPath = path + File.separator + filename;

            //transfer the files into the target folder
            file.transferTo(new File(finalPath));
            return finalPath;
        } else {
            return "file not exist";
        }
    }

    //transfer file
    private File transferFile(MultipartFile file, String path) throws IOException {
        if(!file.isEmpty()) {
            String filename = file.getOriginalFilename();
            File filepath = new File(path,filename);
            return filepath;
        } else {

            throw new IOException("empty multipartfile");
        }
    }

    @RequestMapping(value = "/note", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<String> creates3Notes(@RequestParam(value="sNote", required =false)  String sNote, @RequestParam(value="file", required =false) MultipartFile file,Principal principal, HttpServletRequest req, HttpServletResponse response) throws IOException {
        statsDClient.incrementCounter("_Creates3Note_API_");
        logger.info("Inside__Creates3Note_API_");

        response.setContentType(ContentType.APPLICATION_JSON.getMimeType());
        JsonObject jsonObject = new JsonObject();
        Gson gson = new Gson();
        logger.error("sNote "+ sNote );
        if(sNote.equals("")){
            jsonObject.addProperty("message", "Note can not be blank");
            logger.error("Bad request");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(jsonObject.toString());
        }
        Note note = gson.fromJson(sNote, Note.class);

        if (BasicAuth().equals("unauthorized")) {
            jsonObject.addProperty("message", "You are not logged in!");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(jsonObject.toString());
        } else if (BasicAuth().equals("BadRequest")) {
            jsonObject.addProperty("message", "Please enter valid credentials!");
            logger.error("Bad request");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(jsonObject.toString());
        }

        else{
            if (note.getContent() != null && gson.fromJson(sNote, Note.class).getContent().length() >= 4096) {
                jsonObject.addProperty("message", "note: bad request : size too large");
                logger.error("Bad request");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(jsonObject.toString());
            }
            if(note.getTitle() == null) {
                jsonObject.addProperty("message", "Title can not be blank");
                logger.error("Bad request");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(jsonObject.toString());
            }
            User user;
            user = userRepository.findByEmail(principal.getName());
            note.setUser(user);
            note.setCreated_on(new Date());
            note.setLast_updated_on(new Date());
            noteRepository.save(note);
            if (note == null) {
                jsonObject.addProperty("message", "Note not found!");
                logger.error("Note not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(jsonObject.toString());
            }
            else if(user != note.getUser()){
                jsonObject.addProperty("message", "Unauthorized to access this note!");
                logger.error("Unauthorized");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(jsonObject.toString());
            } else {

                if(file != null) {
                    Attachment attachment = new Attachment();
                    attachment.setNote(note);
                    attachmentRepository.save(attachment);
                    try {
                        String uploadDir = "/uploads/";
                        String realPath2Upload = req.getServletContext().getRealPath(uploadDir);
                        if(! new File(realPath2Upload).exists())
                        {
                            new File(realPath2Upload).mkdir();
                        }
                        String filePath2Upload = realPath2Upload+file.getOriginalFilename();
                        String keyName = note.getId() + ":" + attachment.getId().toString();
                        File saveFile = new File(filePath2Upload);

                        file.transferTo(saveFile);
                        ObjectMetadata metadata = new ObjectMetadata();
                        metadata.setContentLength(file.getSize());
                        InputStream inputStream = new FileInputStream(saveFile);
                        s3client.putObject(new PutObjectRequest(bucketName, keyName, inputStream, metadata).withCannedAcl(CannedAccessControlList.PublicRead));
                        String filename = s3client.getUrl(bucketName, keyName).toString().substring(s3client.getUrl(bucketName, keyName).toString().lastIndexOf("/") + 1);
                        String fileUrl = endpointUrl + "/" + bucketName + "/" + filename;

                        attachment.setPath(fileUrl);
                        attachmentRepository.save(attachment);

                        List<Attachment> attachmentList = new ArrayList<Attachment>();
                        attachmentList.add(attachment);
                        note.setAttachmentList(attachmentList);

                        noteRepository.save(note);

                    } catch (AmazonServiceException ase) {
                        jsonObject.addProperty("Status", "Caught an AmazonServiceException, which " +
                                "means your request made it " +
                                "to Amazon S3, but was rejected with an error response" +
                                " for some reason.");
                        jsonObject.addProperty("Error Message    ", ase.getMessage());
                        jsonObject.addProperty("HTTP Status Code ", ase.getStatusCode());
                        jsonObject.addProperty("AWS Error Code   ", ase.getErrorCode());
                        jsonObject.addProperty("Error Type       ", ase.getErrorType().toString());
                        jsonObject.addProperty("Request ID       ", ase.getRequestId());
                        logger.error("Bad request");
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(jsonObject.toString());

                    } catch (AmazonClientException ace) {
                        jsonObject.addProperty("Status", "Caught an AmazonClientException, which " +
                                "means the client encountered " +
                                "an internal error while trying to " +
                                "communicate with S3, " +
                                "such as not being able to access the network.");
                        jsonObject.addProperty("Error Message: ", ace.getMessage());
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(jsonObject.toString());
                    }
                    jsonObject.addProperty("id", note.getId().toString());
                    jsonObject.addProperty("content", note.getContent());
                    jsonObject.addProperty("title", note.getTitle());
                    jsonObject.addProperty("created On", note.getCreated_on().toString());
                    jsonObject.addProperty("Last updated on", note.getLast_updated_on().toString());
                    JsonArray array = new JsonArray();
                    JsonArray arrayAll = new JsonArray();
                    if (note.getAttachmentList().size() > 0) {
                        for (int j = 0; j < note.getAttachmentList().size();j++) {
                            try {
                                JsonObject eAttach = new JsonObject();
                                Attachment curAttachment = note.getAttachmentList().get(j);
                                eAttach.addProperty("id", curAttachment.getId().toString());
                                eAttach.addProperty("url", curAttachment.getPath());
                                arrayAll.add(eAttach);
                            } catch (Exception exp) {
                                logger.error("Error in deserializing received constraints");
                                System.out.println("Error in deserializing received constraints");
                                return null;
                            }
                        }
                    }
                    jsonObject.add("Attachments", arrayAll);

                    array.add(jsonObject);
                    return ResponseEntity.status(HttpStatus.CREATED).body(array.toString());
                }
                jsonObject.addProperty("id", note.getId().toString());
                jsonObject.addProperty("content", note.getContent());
                jsonObject.addProperty("title", note.getTitle());
                jsonObject.addProperty("created On", note.getCreated_on().toString());
                jsonObject.addProperty("Last updated on", note.getLast_updated_on().toString());
                return ResponseEntity.status(HttpStatus.CREATED).body(jsonObject.toString());
            }
        }
    }


    @RequestMapping(value = "/note/{id}/attachments/{idAttachments}", method = RequestMethod.DELETE)
    @ResponseBody
    public ResponseEntity<String> deleteFile(@PathVariable String id, @PathVariable String idAttachments,Principal principal,  HttpServletResponse response) {
        statsDClient.incrementCounter("_DeleteAttachment_API_");
        logger.info("Inside__DeleteAttachment_API_");
        JsonObject jsonObject = new JsonObject();
        if (BasicAuth().equals("unauthorized")) {
            jsonObject.addProperty("message", "You are not logged in!");
            logger.error("Bad request");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(jsonObject.toString());
        } else if (BasicAuth().equals("BadRequest")) {
            jsonObject.addProperty("message", "Please enter valid credentials!");
            logger.error("Bad request");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(jsonObject.toString());
        } else {
            User user;
            Note note;
            note = noteRepository.findById(UUID.fromString(id));
            user = userRepository.findByEmail(principal.getName());
            if (note == null) {
                jsonObject.addProperty("message", "Note not found!");
                logger.error("Bad request");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(jsonObject.toString());
            }
            else if(user != note.getUser()){
                jsonObject.addProperty("message", "Unauthorized to access this note!");
                logger.error("Bad request");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(jsonObject.toString());
            } else {
                Attachment attachment = attachmentRepository.findById(UUID.fromString(idAttachments));

                if (attachment == null) {
                    jsonObject.addProperty("message", "No attachment found!");
                    logger.error("Bad request");
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(jsonObject.toString());
                }
                try {

                    //AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
                    //s3client = new AmazonS3Client(credentials);
                    System.out.println("Deleting file from S3 bucket!!");
                    s3client.deleteObject(new DeleteObjectRequest(bucketName, note.getId() + ":" + attachment.getId().toString()));
                    attachmentRepository.deleteById(UUID.fromString(idAttachments));
                    jsonObject.addProperty("message", "Attachment deleted successfully!");
                    logger.error("Bad request");
                    return ResponseEntity.status(HttpStatus.NO_CONTENT).body(jsonObject.toString());

                } catch (AmazonServiceException ase) {
                    jsonObject.addProperty("bucket name: " , bucketName);
                    jsonObject.addProperty("Request made to s3 bucket failed","");
                    jsonObject.addProperty("Error Message:    " , ase.getMessage());
                    jsonObject.addProperty("HTTP Status Code: " , ase.getStatusCode());
                    jsonObject.addProperty("AWS Error Code:   " , ase.getErrorCode());
                    jsonObject.addProperty("Error Type:       " , ase.getErrorType().toString());
                    jsonObject.addProperty("Request ID:       " , ase.getRequestId());
                    logger.error("Bad request");
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(jsonObject.toString());
                } catch (Exception e) {
                    jsonObject.addProperty("Message" , "AWS exception");
                    logger.error("Bad request");
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(jsonObject.toString());
                }
            }
        }
    }

    @RequestMapping(value = "/note/{id}", method = RequestMethod.DELETE)
    @ResponseBody
    public ResponseEntity<String> deleteNote(@PathVariable String id,Principal principal,  HttpServletResponse response) {
        statsDClient.incrementCounter("_DeleteS3Note_API_");
        logger.info("Inside__DeleteS3Note_API_");
        JsonObject jsonObject = new JsonObject();
        if (BasicAuth().equals("unauthorized")) {
            jsonObject.addProperty("message", "You are not logged in!");
            logger.error("Bad request");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(jsonObject.toString());
        } else if (BasicAuth().equals("BadRequest")) {
            jsonObject.addProperty("message", "Please enter valid credentials!");
            logger.error("Bad request");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(jsonObject.toString());
        } else {
            User user;
            Note note;
            note = noteRepository.findById(UUID.fromString(id));
            user = userRepository.findByEmail(principal.getName());
            if (note == null) {
                jsonObject.addProperty("message", "Note not found!");
                logger.error("Bad request");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(jsonObject.toString());
            }
            else if(user != note.getUser()){
                jsonObject.addProperty("message", "Unauthorized to access this note!");
                logger.error("Bad request");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(jsonObject.toString());
            } else {


                try {

                    //AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
                    //s3client = new AmazonS3Client(credentials);
                    System.out.println("Deleting file from S3 bucket!!");

                    List<Attachment> attachments = new ArrayList<Attachment>();

                    if (note.getAttachmentList().size() > 0) {
                        for (int i = 0; i < note.getAttachmentList().size(); i++) {
                            try {
                                JsonObject e = new JsonObject();
                                Attachment curAttachment = note.getAttachmentList().get(i);
                                System.out.println("Deleting file from S3 bucket!!");
                                s3client.deleteObject(new DeleteObjectRequest(bucketName, note.getId() + ":" + curAttachment.getId().toString()));
                                attachmentRepository.deleteById(UUID.fromString(curAttachment.getId().toString()));
                            } catch (Exception e) {
                                System.out.println("Error in deserializing received constraints");
                                return null;
                            }
                        }
                    }
                    noteRepository.deleteById(UUID.fromString(id));
                    jsonObject.addProperty("message", "Attachment deleted successfully!");
                    return ResponseEntity.status(HttpStatus.NO_CONTENT).body(jsonObject.toString());

                } catch (AmazonServiceException ase) {
                    jsonObject.addProperty("bucket name: " , bucketName);
                    jsonObject.addProperty("Request made to s3 bucket failed","");
                    jsonObject.addProperty("Error Message:    " , ase.getMessage());
                    jsonObject.addProperty("HTTP Status Code: " , ase.getStatusCode());
                    jsonObject.addProperty("AWS Error Code:   " , ase.getErrorCode());
                    jsonObject.addProperty("Error Type:       " , ase.getErrorType().toString());
                    jsonObject.addProperty("Request ID:       " , ase.getRequestId());
                    logger.error("Bad request");
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(jsonObject.toString());
                } catch (Exception e) {
                    jsonObject.addProperty("Message" , "AWS exception");
                    logger.error("Bad request");
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(jsonObject.toString());
                }
            }
        }
    }


    @RequestMapping(value = "/note/{id}/attachments/{idAttachments}", method = RequestMethod.PUT)
    @ResponseBody
    public ResponseEntity<String> uploadFile(@PathVariable String id, @RequestParam("file") MultipartFile file, @PathVariable String idAttachments,Principal principal, HttpServletRequest req, HttpServletResponse response) throws IOException {

        statsDClient.incrementCounter("_PutAttachment_API_");
        logger.info("Inside__PutAttachment_API_");
        JsonObject jsonObject = new JsonObject();
        if (BasicAuth().equals("unauthorized")) {
            jsonObject.addProperty("message", "You are not logged in!");
            logger.error("Bad request");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(jsonObject.toString());
        } else if (BasicAuth().equals("BadRequest")) {
            jsonObject.addProperty("message", "Please enter valid credentials!");
            logger.error("Bad request");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(jsonObject.toString());
        } else {
            User user;
            Note note;
            note = noteRepository.findById(UUID.fromString(id));
            user = userRepository.findByEmail(principal.getName());
            if (note == null) {
                jsonObject.addProperty("message", "Note not found!");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(jsonObject.toString());
            } else if (user != note.getUser()) {
                jsonObject.addProperty("message", "Unauthorized to access this note!");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(jsonObject.toString());
            } else {
                Attachment attachment = attachmentRepository.findById(UUID.fromString(idAttachments));

                if (attachment == null) {
                    jsonObject.addProperty("message", "No attachment found!");
                    logger.error("Bad request");
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(jsonObject.toString());
                }
                //AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
                //s3client = new AmazonS3Client(credentials);
                //s3client.deleteObject(new DeleteObjectRequest(bucketName, note.getId() + ":" + attachment.getId().toString()));
                s3client.deleteObject(bucketName, note.getId() + ":" + attachment.getId().toString());

                //String folder = "/myFile";
                //String relativePath = System.getProperty("user.dir");
                //String filePath = saveFile(file, "" + relativePath);

                //attachment.setPath(filePath);
                attachment.setNote(note);
                attachmentRepository.save(attachment);
                try {

                    String keyName = note.getId() + ":" + attachment.getId().toString();
                    String uploadDir = "/uploads/";
                    String realPath2Upload = req.getServletContext().getRealPath(uploadDir);
                    if(! new File(realPath2Upload).exists())
                    {
                        new File(realPath2Upload).mkdir();
                    }

                    String filePath2Upload = realPath2Upload+file.getOriginalFilename();

                    File saveFile = new File(filePath2Upload);
                    file.transferTo(saveFile);

                    ObjectMetadata metadata = new ObjectMetadata();
                    metadata.setContentLength(file.getSize());
                    InputStream inputStream = new FileInputStream(saveFile);
                    s3client.putObject(new PutObjectRequest(bucketName, keyName, inputStream, metadata).withCannedAcl(CannedAccessControlList.PublicRead));

                    String filename = s3client.getUrl(bucketName, keyName).toString().substring(s3client.getUrl(bucketName, keyName).toString().lastIndexOf("/") + 1);
                    String fileUrl = endpointUrl + "/" + bucketName + "/" + filename;
                    attachment.setPath(fileUrl);
                    attachmentRepository.save(attachment);

                    jsonObject.addProperty("id", attachment.getId().toString());
                    jsonObject.addProperty("url", attachment.getPath());
                    return ResponseEntity.status(HttpStatus.NO_CONTENT).body(jsonObject.toString());

                } catch (AmazonServiceException ase) {
                    jsonObject.addProperty("Status", "Caught an AmazonServiceException, which " +
                            "means your request made it " +
                            "to Amazon S3, but was rejected with an error response" +
                            " for some reason.");
                    jsonObject.addProperty("Error Message    ", ase.getMessage());
                    jsonObject.addProperty("HTTP Status Code ", ase.getStatusCode());
                    jsonObject.addProperty("AWS Error Code   ", ase.getErrorCode());
                    jsonObject.addProperty("Error Type       ", ase.getErrorType().toString());
                    jsonObject.addProperty("Request ID       ", ase.getRequestId());
                    logger.error("Bad request");
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(jsonObject.toString());

                } catch (AmazonClientException ace) {
                    jsonObject.addProperty("Status", "Caught an AmazonClientException, which " +
                            "means the client encountered " +
                            "an internal error while trying to " +
                            "communicate with S3, " +
                            "such as not being able to access the network.");
                    jsonObject.addProperty("Error Message: ", ace.getMessage());
                    logger.error("Bad request");
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(jsonObject.toString());
                } catch (Exception e) {
                    jsonObject.addProperty("Error Type: ", e.getClass().toString());
                    jsonObject.addProperty("Error Message: ", e.getMessage());
                    logger.error("Bad request");
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(jsonObject.toString());
                }
            }
        }
    }

}