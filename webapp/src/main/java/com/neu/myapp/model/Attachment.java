package com.neu.myapp.model;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.UUID;

@Entity
@Table(name="attachment")
public class Attachment {
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;
    //   private String attachmentId;
    private String path;


    @ManyToOne
    private Note note;

    public Attachment() {

    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

//
//    public String getAttachmentId() {
//        return attachmentId;
//    }
//
//    public void setAttachmentId(String attachmentId) {
//        this.attachmentId = attachmentId;
//    }


    public Note getNote() {
        return note;
    }

    public void setNote(Note note) {
        this.note = note;
    }

    public String getPath() { return path; }

    public void setPath(String path) {
        this.path = path;
    }
}