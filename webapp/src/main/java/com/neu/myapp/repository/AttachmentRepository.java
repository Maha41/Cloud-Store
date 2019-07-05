package com.neu.myapp.repository;

import com.neu.myapp.model.Attachment;
import com.neu.myapp.model.Note;
import org.springframework.data.repository.CrudRepository;
import java.util.List;
import java.util.UUID;

import javax.transaction.*;
@Transactional

public interface AttachmentRepository extends CrudRepository<Attachment, Integer> {
    public List<Attachment> findByNote(Note note);
    public Attachment findById(UUID id);
    public void deleteById(UUID id);
}