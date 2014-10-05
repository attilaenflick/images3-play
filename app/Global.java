

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.images3.ImageS3;
import com.images3.exceptions.AmazonS3BucketAccessFailedException;
import com.images3.exceptions.IllegalResizingDimensionsException;
import com.images3.exceptions.IllegalTemplateNameException;
import com.images3.exceptions.IllegalTemplateNameLengthException;
import com.images3.exceptions.NoSuchEntityFoundException;
import com.images3.exceptions.UnknownImageFormatException;
import com.images3.exceptions.UnremovableTemplateException;
import com.images3.rest.exceptions.ErrorResponse;
import com.images3.rest.exceptions.ExceptionMapper;
import com.images3.rest.exceptions.PreciseExceptionMapper;

import play.Application;
import play.GlobalSettings;
import play.Logger;
import play.libs.F.Promise;
import play.libs.Json;
import play.mvc.Result;
import play.mvc.Http;
import play.mvc.Results;

public class Global extends GlobalSettings {

    private Injector injector;
    private ExceptionMapper exceptionHandler;
    
    public void onStart(Application app) {
        initExceptionHandlers();
        final ImageS3Provider imageS3Provider = new ImageS3Provider(
                app.getFile(app.configuration().getString("images3.conf")).getAbsolutePath(),
                app.getFile(app.configuration().getString("imageprocessor.conf")).getAbsolutePath(),
                app.getFile(app.configuration().getString("mongodb.conf")).getAbsolutePath()
                );
        injector = Guice.createInjector(new AbstractModule() {

            @Override
            protected void configure() {
                bind(ImageS3.class).toProvider(imageS3Provider).asEagerSingleton();
                bind(ObjectMapper.class).toProvider(new ObjectMapperProvider()).asEagerSingleton();;
            }
            
        });
        Logger.info("Application has started");
    }
    
    private void initExceptionHandlers() {
   
        exceptionHandler = new PreciseExceptionMapper(IllegalResizingDimensionsException.class, exceptionHandler) {

            @Override
            protected Result getResult(Throwable t) {
                IllegalResizingDimensionsException exp = (IllegalResizingDimensionsException) t.getCause();
                Map<String, Object> values = new HashMap<String, Object>();
                values.put("width", exp.getWidth());
                values.put("height", exp.getHeight());
                ErrorResponse response = new ErrorResponse(
                        ErrorResponse.ILLEGAL_RESIZING_DIMENSIONS, 
                        values, 
                        exp.getMessage());
                
                return Results.badRequest(Json.toJson(response));
            }
            
        };
        exceptionHandler = new PreciseExceptionMapper(IllegalTemplateNameException.class, exceptionHandler) {

            @Override
            protected Result getResult(Throwable t) {
                IllegalTemplateNameException exp = (IllegalTemplateNameException) t.getCause();
                Map<String, Object> values = new HashMap<String, Object>();
                values.put("name", exp.getName());
                ErrorResponse response = new ErrorResponse(
                        ErrorResponse.ILLEGAL_TEMPLATE_NAME, 
                        values, 
                        exp.getMessage());
                return Results.badRequest(Json.toJson(response));
            }
            
        };
        exceptionHandler = new PreciseExceptionMapper(IllegalTemplateNameLengthException.class, exceptionHandler) {

            @Override
            protected Result getResult(Throwable t) {
                IllegalTemplateNameLengthException exp = (IllegalTemplateNameLengthException) t.getCause();
                Map<String, Object> values = new HashMap<String, Object>();
                values.put("name", exp.getName());
                values.put("minLength", exp.getMinLength());
                values.put("maxLength", exp.getMaxLength());
                ErrorResponse response = new ErrorResponse(
                        ErrorResponse.ILLEGAL_TEMPLATE_NAME_LENGTH, 
                        values, 
                        exp.getMessage());
                return Results.badRequest(Json.toJson(response));
            }
            
        };
        exceptionHandler = new PreciseExceptionMapper(NoSuchEntityFoundException.class, exceptionHandler) {

            @Override
            protected Result getResult(Throwable t) {
                NoSuchEntityFoundException exp = (NoSuchEntityFoundException) t.getCause();
                Map<String, Object> values = new HashMap<String, Object>();
                values.put("entity", exp.getEntity());
                values.put("id", exp.getId());
                ErrorResponse response = new ErrorResponse(
                        ErrorResponse.NO_SUCH_ENTITY, 
                        values, 
                        exp.getMessage());
                return Results.badRequest(Json.toJson(response));
            }
            
        };
        exceptionHandler = new PreciseExceptionMapper(UnknownImageFormatException.class, exceptionHandler) {

            @Override
            protected Result getResult(Throwable t) {
                UnknownImageFormatException exp = (UnknownImageFormatException) t.getCause();
                Map<String, Object> values = new HashMap<String, Object>();
                values.put("imageId", exp.getImageId());
                ErrorResponse response = new ErrorResponse(
                        ErrorResponse.UNKNOWN_IMAGE_FORMAT, 
                        values, 
                        exp.getMessage());
                return Results.badRequest(Json.toJson(response));
            }
            
        };
        exceptionHandler = new PreciseExceptionMapper(UnremovableTemplateException.class, exceptionHandler) {

            @Override
            protected Result getResult(Throwable t) {
                UnremovableTemplateException exp = (UnremovableTemplateException) t.getCause();
                Map<String, Object> values = new HashMap<String, Object>();
                values.put("imagePlantId", exp.getId().getImagePlantId());
                values.put("templateName", exp.getId().getTemplateName());
                ErrorResponse response = new ErrorResponse(
                        ErrorResponse.UNREMOVABLE_TEMPLATE, 
                        values, 
                        exp.getMessage());
                return Results.badRequest(Json.toJson(response));
            }
        };
        exceptionHandler = new PreciseExceptionMapper(AmazonS3BucketAccessFailedException.class, exceptionHandler) {

            @Override
            protected Result getResult(Throwable t) {
                AmazonS3BucketAccessFailedException exp = (AmazonS3BucketAccessFailedException) t.getCause();
                Map<String, Object> values = new HashMap<String, Object>();
                values.put("name", exp.getBucket().getName());
                values.put("accessKey", exp.getBucket().getAccessKey());
                values.put("secretKey", exp.getBucket().getSecretKey());
                ErrorResponse response = new ErrorResponse(
                        ErrorResponse.AMAZONS3_BUCKET_ACCESS_FAILED, 
                        values, 
                        exp.getMessage());
                return Results.badRequest(Json.toJson(response));
            }
        };
    }

    public void onStop(Application app) {
        Logger.info("Application shutdown...");
    }
    
    public <T> T getControllerInstance(Class<T> clazz) throws Exception {
        return injector.getInstance(clazz);
    }
    
    public Promise<Result> onError(Http.RequestHeader request, Throwable t) {
        return Promise.<Result>pure(exceptionHandler.toResult(t));
    }
    
    //public Promise<Result> onHandlerNotFound(Http.RequestHeader request) {
        //return Promise.<Result>pure(Results.notFound("/404.html"));
    //}
    
}
