package com.neu.myapp.repository;


import com.neu.myapp.model.User;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;


@Repository
    public interface UserRepository extends CrudRepository<User, String> {
      public  User findByEmail(String email);
    }


