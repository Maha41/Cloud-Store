package com.neu.myapp.repository;

import com.neu.myapp.model.Note;
import org.springframework.data.repository.CrudRepository;

import javax.transaction.*;
import java.util.List;
import java.util.UUID;


@Transactional
    public interface NoteRepository extends CrudRepository<Note, String>{
        public List<Note> findNoteByUser_id(Integer id);
        public Note findById(UUID id);
        public void deleteById(UUID id);
    }

