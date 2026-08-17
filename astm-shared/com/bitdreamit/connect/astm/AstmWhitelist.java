package com.bitdreamit.connect.astm;

import com.mirth.connect.model.converters.ObjectXMLSerializer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class AstmWhitelist {
    public AstmWhitelist() {
    }

    protected static void whiteListClasses() {
        ObjectXMLSerializer serializer = ObjectXMLSerializer.getInstance();

        try {
            Method method = serializer.getClass().getMethod("allowTypes", List.class, List.class, List.class);
            List<String> allowedTypes = new ArrayList<>();
            allowedTypes.add(AstmReceiverProperties.class.getCanonicalName());
            allowedTypes.add(AstmDispatcherProperties.class.getCanonicalName());
            method.invoke(serializer, allowedTypes, null, null);
            System.out.println("ASTM properties whitelisted successfully in XStream");
        } catch (NoSuchMethodException var3) {
            System.out.println("Old Mirth Connect version, ASTM properties whitelist not required in XStream");
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | SecurityException var4) {
            Exception e = var4;
            ((Exception)e).printStackTrace();
            System.err.println("Error when adding ASTM properties to the whitelist in XStream");
        }

    }
}
