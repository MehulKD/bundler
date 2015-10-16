package in.workarounds.autorikshaw.compiler;

import com.google.auto.service.AutoService;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

import in.workarounds.autorikshaw.annotations.Destination;
import in.workarounds.autorikshaw.annotations.Passenger;
import in.workarounds.autorikshaw.compiler.model.DestinationModel;
import in.workarounds.autorikshaw.compiler.model.PassengerModel;
import in.workarounds.autorikshaw.compiler.support.CustomTypeVisitor;
import in.workarounds.autorikshaw.compiler.support.TypeMatcher;

@AutoService(Processor.class)
public class RikshawProcessor extends AbstractProcessor {
    private Types typeUtils;
    private Elements elementUtils;
    private Filer filer;
    private Messager messager;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        typeUtils = processingEnv.getTypeUtils();
        elementUtils = processingEnv.getElementUtils();
        filer = processingEnv.getFiler();
        messager = processingEnv.getMessager();
    }


    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(Destination.class)) {
            try {
                DestinationModel model = new DestinationModel(element, typeUtils);
            } catch (IllegalArgumentException exception) {
                error(element, exception.getMessage());
            }

            List<PassengerModel> passengers = new ArrayList<>();
            for (Element possiblePassenger : element.getEnclosedElements()) {
                Passenger passenger = possiblePassenger.getAnnotation(Passenger.class);
                if (passenger != null) {
                    PassengerModel passengerModel = new PassengerModel(possiblePassenger);
                    passengerModel.getType().accept(CustomTypeVisitor.getInstance(), passengerModel.parsedType);
                    if(TypeMatcher.finalCheck(passengerModel.parsedType)) {
                        passengers.add(passengerModel);
                    } else {
                        error(possiblePassenger, "Unsupported type found for @%s %s",
                                Passenger.class.getSimpleName(),
                                passengerModel.getLabel());
                    }
                }
            }

        }

        return true;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> annotations = new LinkedHashSet<String>();

        annotations.add(Passenger.class.getCanonicalName());
        annotations.add(Destination.class.getCanonicalName());

        return annotations;
    }

    @Override
    public Set<String> getSupportedOptions() {
        return super.getSupportedOptions();
    }

    public void error(Element e, String msg, Object... args) {
        messager.printMessage(
                Diagnostic.Kind.ERROR,
                String.format(msg, args),
                e);
    }

    public void message(Element e, String msg, Object... args) {
        messager.printMessage(
                Diagnostic.Kind.NOTE,
                String.format(msg, args),
                e);
    }

    public void warn(Element e, String msg, Object... args) {
        messager.printMessage(
                Diagnostic.Kind.WARNING,
                String.format(msg, args),
                e);
    }

}
