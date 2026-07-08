package com.example.app.exception;

public class MovieNotFoundException extends RuntimeException {

    public MovieNotFoundException(Long movieId) {
        super("No rating found for movie " + movieId);
    }
}
