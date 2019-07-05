package com.neu.myapp.model;


import javax.persistence.*;
import java.util.List;
import com.neu.myapp.model.Note;

@Entity
@Table(name="user")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;
    @Column(name = "name")
    private String name;
    @Column(name = "password")
    private String password;
    @Column(name = "email")
    private String email;
    @OneToMany(mappedBy = "user",orphanRemoval=true, cascade=CascadeType.ALL)
    private List<Note> Notes;

    public User() {

    }

    public User(User user) {
        this.id=user.getId();
        this.name=user.getName();
        this.email=user.getEmail();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public List<Note> getNotes() {
        return Notes;
    }

    public void setNotes(List<Note> notes) {
        Notes = notes;
    }

    @Override
    public String toString() {
        return "Model{" +
                "model:" + name + "}";
    }
}
