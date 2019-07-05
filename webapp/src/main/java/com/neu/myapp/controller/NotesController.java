package com.neu.myapp.controller;

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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
@RestController
public class NotesController {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private NoteRepository noteRepository;


    @Autowired
    private AttachmentRepository attachmentRepository;

    @Autowired
    private StatsDClient statsDClient;

    private final static Logger logger= LoggerFactory.getLogger(NotesController.class);


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

    @RequestMapping(value = "/lnote", method = RequestMethod.POST, produces = "application/json")
    @ResponseBody
    public ResponseEntity<String> createNotes(@RequestParam(value="sNote", required =false)  String sNote, @RequestParam(value="file", required =false) MultipartFile file,Principal principal, HttpServletRequest request, HttpServletResponse response) throws IOException {
        statsDClient.incrementCounter("_CreateNote_API_");
        logger.info("Inside__CreateNote_API_");

        response.setContentType(ContentType.APPLICATION_JSON.getMimeType());
        JsonObject jsonObject = new JsonObject();
        Gson gson = new Gson();
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
            if(note.getContent() == null) {
                jsonObject.addProperty("message", "Content  can not be blank");
                logger.error("Bad request");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(jsonObject.toString());
            }
            User user;
            user = userRepository.findByEmail(principal.getName());
            note.setUser(user);
            note.setCreated_on(new Date());
            note.setLast_updated_on(new Date());

            noteRepository.save(note);

            if(file != null) {
                String relativePath = System.getProperty("user.dir");
                String folder= "MyFile";
                String filePath = saveFile(file, "" + relativePath+folder);

                Attachment attachment = new Attachment();
                attachment.setPath(filePath);
                attachment.setNote(note);
                attachmentRepository.save(attachment);

                List<Attachment> attachmentList = new ArrayList<Attachment>();
                attachmentList.add(attachment);
                note.setAttachmentList(attachmentList);

                noteRepository.save(note);
            }
            jsonObject.addProperty("id", note.getId().toString());
            jsonObject.addProperty("content", note.getContent());
            jsonObject.addProperty("title", note.getTitle());
            jsonObject.addProperty("created On", note.getCreated_on().toString());
            jsonObject.addProperty("Last updated on", note.getLast_updated_on().toString());
            List<Attachment> attachments = new ArrayList<Attachment>();
            JsonArray array = new JsonArray();
            if (note.getAttachmentList().size() > 0) {
                for (int i = 0; i < note.getAttachmentList().size(); i++) {
                    try {
                        JsonObject e = new JsonObject();
                        Attachment curAttachment = note.getAttachmentList().get(i);
                        e.addProperty("id", curAttachment.getId().toString());
                        e.addProperty("url", curAttachment.getPath());
                        array.add(e);
                    } catch (Exception e) {
                        System.out.println("Error in deserializing received constraints");
                        return null;
                    }
                }
            }
//                attachmentJson.addProperty("id",note.getAttachmentList().get(0).getId());
//                attachmentJson.addProperty("url",note.getAttachmentList().get(0).getPath());
            jsonObject.add("Attachments", array);
            return ResponseEntity.status(HttpStatus.CREATED).body(jsonObject.toString());
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

    @RequestMapping(value = "/lnote/{id}", method = RequestMethod.DELETE, produces = "application/json")
    @ResponseBody
    public ResponseEntity<String> deleteNote(@PathVariable String id, Principal principal, HttpServletResponse response) {
        statsDClient.incrementCounter("_DeleteNote_API_");
        logger.info("Inside_DeleteNote_API_");
        logger.warn("Deleting note");
        response.setContentType(ContentType.APPLICATION_JSON.getMimeType());
        JsonObject jsonObject = new JsonObject();
        Note note;
        User user;
        if (BasicAuth().equals("unauthorized")) {
            jsonObject.addProperty("message", "You are not logged in!");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(jsonObject.toString());
        } else if (BasicAuth().equals("BadRequest")) {
            jsonObject.addProperty("message", "Please enter valid credentials!");
            logger.error("Please enter valid credentials!");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(jsonObject.toString());
        } else {
            note = noteRepository.findById(UUID.fromString(id));
            user = userRepository.findByEmail(principal.getName());
            if (note == null) {
                jsonObject.addProperty("message", "Note not found!");
                logger.error("Note not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(jsonObject.toString());
            }
            else if(user != note.getUser()){
                jsonObject.addProperty("message", "Unauthorized to access this note!");
                logger.error("User not found");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(jsonObject.toString());
            }
            noteRepository.deleteById(UUID.fromString(id));
            jsonObject.addProperty("message", "Delete note " + id + " successfully! ");
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body(jsonObject.toString());
        }
    }

    @RequestMapping(value = "/note/{id}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ResponseEntity<String> getNote(@PathVariable String id, Principal principal, HttpServletResponse response) {

        statsDClient.incrementCounter("_GetAllAttachments_API_");
        logger.info("Inside_GetAllAttachments_API_");
        response.setContentType(ContentType.APPLICATION_JSON.getMimeType());
        JsonObject jsonObject = new JsonObject();
        JsonObject attachmentJson = new JsonObject();
        if (BasicAuth().equals("unauthorized")) {
            jsonObject.addProperty("message", "You are not logged in!");
            logger.error("You are not logged in!");
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
                logger.error("Note not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(jsonObject.toString());
            }
            else if(user != note.getUser()){
                jsonObject.addProperty("message", "Unauthorized to access this note!");
                logger.error("User not found");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(jsonObject.toString());
            } else {
                jsonObject.addProperty("id", note.getId().toString());
                jsonObject.addProperty("content", note.getContent());
                jsonObject.addProperty("title", note.getTitle());
                jsonObject.addProperty("created On", note.getCreated_on().toString());
                jsonObject.addProperty("Last updated on", note.getLast_updated_on().toString());
                List<Attachment> attachments = new ArrayList<Attachment>();
                JsonArray array = new JsonArray();
                if (note.getAttachmentList().size() > 0) {
                    for (int i = 0; i < note.getAttachmentList().size(); i++) {
                        try {
                            JsonObject e = new JsonObject();
                            Attachment curAttachment = note.getAttachmentList().get(i);
                            e.addProperty("id", curAttachment.getId().toString());
                            e.addProperty("url", curAttachment.getPath());
                            array.add(e);
                        } catch (Exception e) {
                            System.out.println("Error in deserializing received constraints");
                            return null;
                        }
                    }
                }
//                attachmentJson.addProperty("id",note.getAttachmentList().get(0).getId());
//                attachmentJson.addProperty("url",note.getAttachmentList().get(0).getPath());
                jsonObject.add("Attachments", array);
                return ResponseEntity.status(HttpStatus.OK).body(jsonObject.toString());
            }
        }
    }

    @RequestMapping(value = "/note", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<String> listNotes(Principal principal, HttpServletResponse response) {
        statsDClient.incrementCounter("_GetNote_API_");
        logger.info("Inside_GetNote_API_");
        JsonObject jsonObject = new JsonObject();
        User user;

        if (BasicAuth().equals("unauthorized")) {
            jsonObject.addProperty("message", "You are not logged in!");
            logger.error("User not found");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(jsonObject.toString());
        } else if (BasicAuth().equals("BadRequest")) {
            jsonObject.addProperty("message", "Please enter valid credentials!");
            logger.error("Please enter valid credentials!");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(jsonObject.toString());
        } else {
            user = userRepository.findByEmail(principal.getName());

            List<Note> notes = noteRepository.findNoteByUser_id(user.getId());
            JsonArray array = new JsonArray();
            if (notes.size() > 0) {
                for (int i = 0; i < notes.size(); i++) {
                    try {
                        JsonObject e = new JsonObject();
                        Note curNote = notes.get(i);
                        e.addProperty("id", curNote.getId().toString());
                        e.addProperty("content", curNote.getContent());
                        e.addProperty("title", curNote.getTitle());
                        e.addProperty("created On", curNote.getCreated_on().toString());
                        e.addProperty("Last updated on", curNote.getLast_updated_on().toString());

                        JsonArray arrayAll = new JsonArray();
                        if (curNote.getAttachmentList().size() > 0) {
                            for (int j = 0; j < curNote.getAttachmentList().size();j++) {
                                try {
                                    JsonObject eAttach = new JsonObject();
                                    Attachment curAttachment = curNote.getAttachmentList().get(j);
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
                        e.add("Attachments", arrayAll);
                        array.add(e);
                    } catch (Exception e) {
                        System.out.println("Error in deserializing received constraints");
                        return null;
                    }
                }
            }
            return ResponseEntity.status(HttpStatus.OK).body(array.toString());
        }
    }

    @RequestMapping(value = "/note/{id}", method = RequestMethod.PUT, produces = "application/json")
    @ResponseBody
    public ResponseEntity<String> updateNotes(@PathVariable("id") String id, @RequestBody String sNote, Principal principal, HttpServletResponse response) {
        statsDClient.incrementCounter("_UpdateNote_API_");
        logger.info("Inside_UpdateNote_API_");
        response.setContentType(ContentType.APPLICATION_JSON.getMimeType());
        JsonObject jsonObject = new JsonObject();
        Gson gson = new Gson();
        if (BasicAuth().equals("unauthorized")) {
            jsonObject.addProperty("message", "You are not logged in!");
            logger.error("You are not logged in!");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(jsonObject.toString());
        } else if (BasicAuth().equals("BadRequest")) {
            jsonObject.addProperty("message", "Please enter valid credentials!");
            logger.error("Please enter valid credentials!");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(jsonObject.toString());
        } else {

            Note newNote = gson.fromJson(sNote, Note.class);
            if (newNote.getContent()!=null && gson.fromJson(sNote, Note.class).getContent().length() >= 4096) {
                jsonObject.addProperty("message", "note: bad request : size too large");
                logger.error("note: bad request : size too large");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(jsonObject.toString());
            }
            User user;

            Note note = noteRepository.findById(UUID.fromString(id));
            user = userRepository.findByEmail(principal.getName());

            if (note == null) {
                jsonObject.addProperty("message", "Note not found!");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(jsonObject.toString());
            }
            else if(user != note.getUser()){
                jsonObject.addProperty("message", "Unauthorized to access this note!");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(jsonObject.toString());
            }

            if(newNote.getTitle() != null) {
                note.setTitle(newNote.getTitle());
            }
            if(newNote.getContent() != null) {
                note.setContent(newNote.getContent());
            }
            if (newNote.getContent()==null && newNote.getTitle()==null) {
                jsonObject.addProperty("message", "Please enter either content or title for the note");
                logger.error("Please enter either content or title for the note");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(jsonObject.toString());
            }
            note.setLast_updated_on(new Date());
            noteRepository.save(note);
            jsonObject.addProperty("id", note.getId().toString());
            jsonObject.addProperty("content", note.getContent());
            jsonObject.addProperty("title", note.getTitle());
            jsonObject.addProperty("created On", note.getCreated_on().toString());
            jsonObject.addProperty("Last updated on", note.getLast_updated_on().toString());
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body(jsonObject.toString());
        }

    }


}