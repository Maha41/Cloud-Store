package com.neu.myapp.model;
import com.neu.myapp.repository.UserRepository;
import com.neu.myapp.model.User;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.util.Date;
import java.util.List;
import java.util.UUID;


@Entity
@Table(name="note")
public class Note {
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "Title")
    private String title;

    @Size(max = 4096)
    private String content;

    @Column(name = "CreatedTime")
    private Date created_on;

    @Column(name = "LastUpdatedTime")
    private Date last_updated_on;


    @ManyToOne
    private User user;

    @OneToMany(mappedBy = "note",orphanRemoval=true, cascade=CascadeType.ALL)
    private List<Attachment> attachmentList;

    public Note() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public List<Attachment> getAttachmentList() {
        return attachmentList;
    }

    public void setAttachmentList(List<Attachment> attachmentList) {
        this.attachmentList = attachmentList;
    }

    public Attachment addAttachment(){
        Attachment a = new Attachment();
        attachmentList.add(a);
        return a;
    }

    public Date getCreated_on() {
        return created_on;
    }

    public void setCreated_on(Date created_on) {
        this.created_on = created_on;
    }

    public Date getLast_updated_on() {
        return last_updated_on;
    }

    public void setLast_updated_on(Date last_updated_on) {
        this.last_updated_on = last_updated_on;
    }
}
