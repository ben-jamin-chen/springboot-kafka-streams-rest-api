package com.example.app.domain.movie;

import com.example.app.domain.movie.dto.MovieAverageRatingResponse;
import com.example.app.exception.GlobalErrorHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springdoc.core.annotations.RouterOperation;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

@Configuration(proxyBeanMethods = false)
public class MovieRoutes {

    @Bean
    @RouterOperation(path = "/v1/movie/{movieId}/rating", method = RequestMethod.GET,
            operation = @Operation(operationId = "getMovieAverageRating",
                    summary = "Returns the average rating for a particular movie",
                    parameters = @Parameter(name = "movieId", in = ParameterIn.PATH,
                            description = "Movie identifier", required = true, example = "362"),
                    responses = {
                            @ApiResponse(responseCode = "200", description = "successful operation",
                                    content = @Content(schema = @Schema(implementation = MovieAverageRatingResponse.class))),
                            @ApiResponse(responseCode = "400", description = "invalid movie identifier"),
                            @ApiResponse(responseCode = "404", description = "no rating found for movie"),
                            @ApiResponse(responseCode = "500", description = "internal server error") }))
    RouterFunction<ServerResponse> movieRouterFunction(MovieHandler movieHandler, GlobalErrorHandler errorHandler) {
        return RouterFunctions.route()
                .GET("/v1/movie/{movieId}/rating", movieHandler::getAverageRating)
                .onError(Exception.class, errorHandler::handle)
                .build();
    }
}
