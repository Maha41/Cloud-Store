package com.neu.myapp.controller;


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

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
public class AttachmentController {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private NoteRepository noteRepository;
    @Autowired
    private AttachmentRepository attachmentRepository;


    @Autowired
    private StatsDClient statsDClient;

    private final static Logger logger= LoggerFactory.getLogger(UserController.class);

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

    @RequestMapping(value = "/lnote/{id}/attachments", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<String> attachFile(@PathVariable String id, @RequestParam(value="file", required =false) MultipartFile file,Principal principal, HttpServletResponse response) throws Exception {

        statsDClient.incrementCounter("_PostNote_API_");
        logger.info("Inside__PostNote_API_");
        response.setContentType(ContentType.APPLICATION_JSON.getMimeType());
        JsonObject jsonObject = new JsonObject();
        Note note;
        if (BasicAuth().equals("unauthorized")) {
            jsonObject.addProperty("message", "You are not logged in!");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(jsonObject.toString());
        } else if (BasicAuth().equals("BadRequest")) {
            jsonObject.addProperty("message", "Please enter valid credentials!");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(jsonObject.toString());
        } else {
            User user;
            note = noteRepository.findById(UUID.fromString(id));
            user = userRepository.findByEmail(principal.getName());
            if (note == null) {
                jsonObject.addProperty("message", "Note not found!");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(jsonObject.toString());
            }
            else if(user != note.getUser()){
                jsonObject.addProperty("message", "Unauthorized to access this note!");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(jsonObject.toString());
            } else {

                note = noteRepository.findById(UUID.fromString(id));
                if (note == null) {
                    jsonObject.addProperty("message", "note does not exist");
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(jsonObject.toString());
                }
                if (file != null) {
                    String relativePath = System.getProperty("user.dir");
                    String folder= "/MyFile";
                    String filePath = saveFile(file, "" + relativePath+folder);
                    Attachment attachment = new Attachment();
                    attachment.setPath(filePath);
                    attachment.setNote(note);
                    attachmentRepository.save(attachment);
                    jsonObject.addProperty("id", attachment.getId().toString());
                    jsonObject.addProperty("url", attachment.getPath());
                    return ResponseEntity.status(HttpStatus.CREATED).body(jsonObject.toString());
                }
                jsonObject.addProperty("Message", "Please attach file to upload!!");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(jsonObject.toString());
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

    @RequestMapping(value = "/lnote/{id}/attachments/{idAttachments}", method = RequestMethod.DELETE)
    @ResponseBody
    public ResponseEntity<String> deleteFile(@PathVariable String id, @PathVariable String idAttachments,Principal principal, HttpServletResponse response) throws Exception {
        statsDClient.incrementCounter("_DeleteNote_API_");
        logger.info("Inside__DeleteNote_API_");
        JsonObject jsonObject = new JsonObject();
        if (BasicAuth().equals("unauthorized")) {
            jsonObject.addProperty("message", "You are not logged in!");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(jsonObject.toString());
        } else if (BasicAuth().equals("BadRequest")) {
            jsonObject.addProperty("message", "Please enter valid credentials!");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(jsonObject.toString());
        } else {
            User user;
            Note note;
            note = noteRepository.findById(UUID.fromString(id));
            user = userRepository.findByEmail(principal.getName());
            if (note == null) {
                jsonObject.addProperty("message", "Note not found!");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(jsonObject.toString());
            }
            else if(user != note.getUser()){
                jsonObject.addProperty("message", "Unauthorized to access this note!");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(jsonObject.toString());
            } else {
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                response.setContentType(ContentType.APPLICATION_JSON.getMimeType());
                Attachment attachment = attachmentRepository.findById(UUID.fromString(idAttachments));
                String filePath = attachment.getPath();
                if (attachment == null) {
                    jsonObject.addProperty("message", "No attachment found!");
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(jsonObject.toString());
                }
                boolean deleteSuccess = false;
                if (attachment.getNote() == note) {
                    deleteSuccess = delete(filePath);
                    if (deleteSuccess) {
                        attachmentRepository.deleteById(UUID.fromString(idAttachments));
                        return ResponseEntity.status(HttpStatus.NO_CONTENT).body("Deleted Successfully!!");
                    }
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("attachement not found!");
                } else {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("attachement not found!");
                }
            }
        }
    }
    private static boolean delete(String fileName) {
        File file = new File(fileName);
        if (!file.exists()) {
            System.out.println("[log] Delete File failed:" + fileName + "not exist！");
            return false;
        } else {
            if (file.isFile())
                return file.delete();
        }
        return false;
    }
    @RequestMapping(value = "/note/{id}/attachments", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<String> listFile(@PathVariable String id, Principal principal) throws Exception {
        statsDClient.incrementCounter("_GetAllAttachemnts_API_");
        logger.info("Inside__GetAllAttachemnts_API_");
        JsonObject jsonObject = new JsonObject();
        if (BasicAuth().equals("unauthorized")) {
            jsonObject.addProperty("message", "You are not logged in!");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(jsonObject.toString());
        } else if (BasicAuth().equals("BadRequest")) {
            jsonObject.addProperty("message", "Please enter valid credentials!");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(jsonObject.toString());
        }

        else {
            User user;
            Note note;
            note = noteRepository.findById(UUID.fromString(id));
            user = userRepository.findByEmail(principal.getName());
            if (note == null) {
                jsonObject.addProperty("message", "Note not found!");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(jsonObject.toString());
            }
            else if(user != note.getUser()){
                jsonObject.addProperty("message", "Unauthorized to access this note!");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(jsonObject.toString());
            } else {
                List<Attachment> attachList = attachmentRepository.findByNote(note);

                JsonArray array = new JsonArray();
                if (attachList.size() > 0) {
                    for (int i = 0; i < attachList.size(); i++) {
                        try {
                            JsonObject e = new JsonObject();
                            Attachment curAttachment = attachList.get(i);
                            e.addProperty("id", curAttachment.getId().toString());
                            e.addProperty("url", curAttachment.getPath());
                            array.add(e);
                        } catch (Exception e) {
                            return null;
                        }
                    }
                    return ResponseEntity.status(HttpStatus.OK).body(array.toString());
                }
                jsonObject.addProperty("message", "Attachments not available for this note !!");
                return ResponseEntity.status(HttpStatus.OK).body(jsonObject.toString());
            }
        }
    }


    @RequestMapping(value = "/lnote/{id}/attachments/{idAttachments}", method = RequestMethod.PUT)
    @ResponseBody
    public ResponseEntity<String> updateFile(@PathVariable String id, @RequestParam("file") MultipartFile file, @PathVariable String idAttachments, Principal principal, HttpServletResponse response) throws Exception {
        statsDClient.incrementCounter("_ReplaceFileAttachemnts_API_");
        logger.info("Inside__ReplaceFileAttachemnts_API_");
        JsonObject jsonObject = new JsonObject();
        if (BasicAuth().equals("unauthorized")) {
            jsonObject.addProperty("message", "You are not logged in!");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(jsonObject.toString());
        } else if (BasicAuth().equals("BadRequest")) {
            jsonObject.addProperty("message", "Please enter valid credentials!");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(jsonObject.toString());
        }

        else {

            Attachment attachment = attachmentRepository.findById(UUID.fromString(idAttachments));
            User user;
            Note note;
            note = noteRepository.findById(UUID.fromString(id));
            user = userRepository.findByEmail(principal.getName());
            if (note == null) {
                jsonObject.addProperty("message", "Note not found!");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(jsonObject.toString());
            }
            else if(user != note.getUser()){
                jsonObject.addProperty("message", "Unauthorized to access this note!");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(jsonObject.toString());
            } else {
                if (attachment == null) {
                    jsonObject.addProperty("message", "No attachment found!");
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(jsonObject.toString());
                }
                delete(attachment.getPath());
                String relativePath = System.getProperty("user.dir");
                String folder= "/MyFile";
                String filePath = saveFile(file, "" + relativePath+folder);
                attachment.setPath(filePath);

                attachmentRepository.save(attachment);
                jsonObject.addProperty("path", attachment.getPath());
                jsonObject.addProperty("task", attachment.getNote().toString());
                jsonObject.addProperty("attachment_id", attachment.getId().toString());

                return ResponseEntity.status(HttpStatus.NO_CONTENT).body(jsonObject.toString());
            }
        }
    }

}