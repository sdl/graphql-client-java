import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sdl.web.pca.client.DefaultGraphQLClient;
import com.sdl.web.pca.client.GraphQLClient;
import com.sdl.web.pca.client.exception.GraphQLClientException;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Formatter;
import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

public class Program {
    private static final Logger LOG = getLogger(Program.class);
    static StringBuilder importBuilder = new StringBuilder();

    public static void main(String[] args) throws GraphQLClientException {
        System.out.print("Generate Model Classes \n");
        if (args.length > 0) {
            String endpoint = null;
            String outputFile = null;
            String ns = null;
            for (int i = 0; i < args.length; i += 2) {
                String argumt = args[i].toLowerCase();
                switch (argumt) {
                    case "-ns":
                        ns = args[i + 1];
                        break;
                    case "-e":
                        endpoint = args[i + 1];
                        break;
                    case "-o":
                        outputFile = args[i + 1];
                        break;
                }
            }
            if (isNullOrBlank(endpoint)) {
                LOG.debug("Specify GraphQL endpoint address.");
                return;
            }
            if (isNullOrBlank(outputFile)) {
                LOG.debug("Specify output file.");
                return;
            }
            if (isNullOrBlank(ns)) {
                LOG.debug("Specify namespace.");
                return;
            }

            GraphQLClient client = new DefaultGraphQLClient(endpoint, null);
            String jsonResponse = "";
            String jsonRequest = "";
            try {
                String query = IOUtils.toString(Program.class.getClassLoader().getResourceAsStream("IntrospectionQuery.graphql"), "UTF-8");
                JsonObject body = new JsonObject();
                body.addProperty("query", query);
                jsonRequest = body.toString();
                jsonResponse = client.execute(jsonRequest);
                JsonParser parser = new JsonParser();
                JsonObject jsonObject = (JsonObject) parser.parse(jsonResponse);
                JsonObject dataObject = jsonObject.getAsJsonObject("data").getAsJsonObject("__schema");
                ObjectMapper objectMapper = new ObjectMapper();
                GraphQLSchema schema = objectMapper.readValue(dataObject.toString(), GraphQLSchema.class);

                generateSchemaClasses(schema, ns, outputFile);
                LOG.debug("GraphQL Schema : " + schema.toString());
            } catch (Exception e) {
                LOG.trace("Cannot generate schema for request '{}' with response '{}'", jsonRequest, jsonResponse, e);
                throw new GraphQLClientException("JSON Mapping Exception : ", e);
            }
        }
    }

    static void generateSchemaClasses(GraphQLSchema schema, String ns, String outputFile) throws IOException {

        for (GraphQLSchemaType type : schema.types) {
            if (type.name == null || type.name.startsWith("__"))
                continue;
            if (type.kind.equalsIgnoreCase("SCALAR"))
                continue;

            StringBuilder sb = new StringBuilder();
            emitPackage(sb, ns);

            emitImport(sb, type, ns);

            StringBuilder sbuilder = generateClass(sb, schema, type, 1);
            createJavaFile(type, sbuilder, outputFile);
        }
    }

    static void createJavaFile(GraphQLSchemaType type, StringBuilder sb, String outputFilePath) throws IOException {
        String filePath = outputFilePath + type.name + ".java";
        File file = new File(filePath);
        if (file.exists())
            file.delete();

        File newfile = new File(filePath);
        if (!newfile.getParentFile().exists()) {
            newfile.getParentFile().mkdir();
        }
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(newfile));
            writer.append(sb);
            LOG.debug("Create Java files in location : " + filePath);

        } finally {
            if (writer != null) writer.close();
        }
    }

    static void emitComment(StringBuilder sb, String comment, int indentCount) {
        if (isNullOrBlank(comment)) return;
        String indentString = new String(new char[indentCount]).replace("\0", "\t");
        sb.append(indentString);
        sb.append("\n/**");
        sb.append("\n");
        sb.append(indentString);
        sb.append("*");
        sb.append(comment);
        sb.append("\n");
        sb.append(indentString);
        sb.append("*/");
        sb.append("\n");
    }

    static StringBuilder generateClass(StringBuilder sb, GraphQLSchema schema, GraphQLSchemaType type, int indentCount) {
        String indentString = new String(new char[indentCount]).replace("\0", "\t");

        emitComment(sb, type.description, indentCount - 1);

        if (type.kind.equalsIgnoreCase("ENUM"))
            sb.append("public enum " + type.name);
        else if (type.kind.equalsIgnoreCase("INTERFACE"))
            sb.append("public interface " + type.name);
        else if (type.interfaces == null || type.interfaces.size() == 0)
            sb.append("public class " + type.name);
        else
            sb.append("public class " + type.name + getImplementation(type.interfaces));
        sb.append(" {");
        sb.append("\n");

        switch (type.kind) {
            case "OBJECT":
                emitFields(sb, type.fields, indentCount + 1, true);
                break;
            case "INPUT_OBJECT":
                emitFields(sb, type.inputFields, indentCount + 1, true);
                break;
            case "INTERFACE":
                if (type.possibleTypes != null) {
                    emitFields(sb, type.fields, indentCount + 1, false);
                }
                break;
            case "ENUM":
                emitFields(sb, type.enumValues, indentCount + 1);
                break;
            default:
                System.out.println("oops, unknown type " + type.kind + " for " + type);
                break;
        }

        sb.append(indentString);
        sb.append("\n");
        sb.append("}\n");
        return sb;
    }

    static void emitFields(StringBuilder sb, List<GraphQLSchemaEnum> enumValues, int indentCount) {
        if (enumValues == null) return;
        String indentString = new String(new char[indentCount]).replace("\0", "\t");
        for (int i = 0; i <= enumValues.size() - 1; i++) {
            sb.append("\n");
            sb.append(indentString);
            if (i >= enumValues.size() - 1)
                sb.append(enumValues.get(i).name);
            else
                sb.append(enumValues.get(i).name + ",");
        }
    }

    static void emitFields(StringBuilder sb, List<GraphQLSchemaField> fields, int indentCount, Boolean isPublic) {
        if (fields == null) return;
        String indentString = new String(new char[indentCount]).replace("\0", "\t");
        for (GraphQLSchemaField field : fields) {
            field.type = remapFieldType(field);
            String returnTypeName = getFieldReturnTypeName(field.type);
            String defaultValue = getDefaultValue(returnTypeName);

            if (field.name.equalsIgnoreCase("abstract")) {
                // avoid using keyword
                field.name = "Abstract";
            }

            sb.append(indentString);
            if (isPublic) {
                sb.append("private " + returnTypeName + " " + field.name + ";");
                sb.append("\n");
            }
        }

        /*Setter & Getter for Model Class*/

        for (GraphQLSchemaField field : fields) {
            field.type = remapFieldType(field);
            String returnTypeName = getFieldReturnTypeName(field.type);
            if (field.name.equalsIgnoreCase("abstract")) {
                // avoid using keyword
                field.name = "Abstract";
            }
            sb.append("\n\n");
            sb.append(indentString);

            if (isPublic) {
                sb.append("public " + returnTypeName + " get" + field.name.substring(0, 1).toUpperCase() + field.name.substring(1) + "()");

                sb.append("{\n");
                sb.append(indentString);
                sb.append("\t");
                sb.append("return " + field.name + ";");
                sb.append("\n");
                sb.append(indentString);
                sb.append("}\n");
                sb.append(indentString);

                sb.append("public void set" + field.name.substring(0, 1).toUpperCase() + field.name.substring(1) + "(" + returnTypeName + " " + field.name + ")");

                sb.append("{\n");
                sb.append(indentString);
                sb.append("\t");
                sb.append("this." + field.name + " = " + field.name + ";");
                sb.append("\n");
                sb.append(indentString);
                sb.append("}\n");
            } else {
                sb.append(returnTypeName + " get" + field.name.substring(0, 1).toUpperCase() + field.name.substring(1) + "()" + ";");
                sb.append("\n");
                sb.append(indentString);
                sb.append("void set" + field.name.substring(0, 1).toUpperCase() + field.name.substring(1) + "(" + returnTypeName + " " + field.name + ")" + ";");
            }
        }
    }

    static String getFieldReturnTypeName(GraphQLSchemaTypeInfo type) {
        switch (type.kind) {
            case "LIST":
                return "List<" + type.ofType.name + ">";
            case "NON_NULL":
                return type.ofType.name;
            default:
                return type.name;
        }
    }

    static String getDefaultValue(String returnType) {
        switch (returnType) {
            case "int":
                return "0";
            case "boolean":
                return "false";
            default:
                return "null";
        }
    }

    static StringBuilder getImplementation(List<GraphQLSchemaInterface> interfaces) {
        StringBuilder impl = null;
        if (interfaces.size() != 0) {
            impl = new StringBuilder();
            impl.append(" implements ");
            String prefix = "";
            for (GraphQLSchemaInterface type : interfaces) {
                impl.append(prefix);
                prefix = ",";
                impl.append(type.name);
            }
        }
        return impl;
    }

    static GraphQLSchemaTypeInfo remapFieldType(GraphQLSchemaField field) {
        // Just remap itemType and namespaceId(s) from int to use our enum
        // to make things a little nicer to work with.
        GraphQLSchemaTypeInfo graphQLSchemaTypeInfo = new GraphQLSchemaTypeInfo();
        if (field.type.name != null) {
            switch (field.type.name) {
                case "Int": {
                    field.type.name = "int";
                    break;
                }
                case "Boolean": {
                    field.type.name = "boolean";
                    break;
                }
                case "Map": {
                    importBuilder.append("import java.util.Map;\n");
                    break;
                }
            }

        }

        if (field.type.name == null) {
            switch (field.type.ofType.name) {
                case "Int": {
                    field.type.ofType.name = "int";
                }
            }
        }

        switch (field.name) {
            case "namespaceIds": {
                GraphQLSchemaTypeInfo ofType = new GraphQLSchemaTypeInfo();
                ofType.kind = "SCALAR";
                ofType.name = "Integer";

                graphQLSchemaTypeInfo.kind = "LIST";
                graphQLSchemaTypeInfo.ofType = ofType;
                return graphQLSchemaTypeInfo;
            }
            case "namespaceId": {
                graphQLSchemaTypeInfo.kind = "SCALAR";
                graphQLSchemaTypeInfo.name = "int";
                return graphQLSchemaTypeInfo;
            }
//            case "itemType": {
//                graphQLSchemaTypeInfo.kind = "ENUM";
//                graphQLSchemaTypeInfo.name = "ItemType";
//                return graphQLSchemaTypeInfo;
//            }
            case "id": {
                graphQLSchemaTypeInfo.kind = "SCALAR";
                graphQLSchemaTypeInfo.name = "String";
                return graphQLSchemaTypeInfo;
            }
            case "data": {
                graphQLSchemaTypeInfo.kind = "SCALAR";
                graphQLSchemaTypeInfo.name = "Map";
                return graphQLSchemaTypeInfo;
            }
            case "publicationIds": {
                graphQLSchemaTypeInfo.kind = "SCALAR";
                graphQLSchemaTypeInfo.name = "List<Integer>";
                return graphQLSchemaTypeInfo;
            }
            default:
                return field.type;
        }
    }

    static StringBuilder emitPackage(StringBuilder sb, String ns) {
        Formatter fmt = new Formatter(sb);
        fmt.format("package %s", ns);
        sb.append(";");
        sb.append("\n");
        sb.append("\n");
        return sb;
    }

    static StringBuilder emitImport(StringBuilder sb, GraphQLSchemaType type, String ns) {
        int count = 0, itemtypeCount = 0;
        if (type.fields != null) {
            for (GraphQLSchemaField field : type.fields) {
                if (field.type.kind != null && count <= 0) {
                    switch (field.type.kind) {
                        case "LIST": {
                            sb.append("import java.util.List;\n");
                            count++;
                            break;
                        }
                    }
                }
                if (field.type.name != null) {
                    switch (field.type.name) {
                        case "Map": {
                            sb.append("import java.util.Map;\n");
                            break;
                        }
                    }
                }
            }
        } else if (type.inputFields != null) {
            for (GraphQLSchemaField field : type.inputFields) {
                if (field.type.kind != null && count < 1) {
                    switch (field.type.kind) {
                        case "LIST": {
                            sb.append("import java.util.List;\n");
                            count++;
                            break;
                        }
                    }
                }
            }
        }
        return sb;
    }

    public static boolean isNullOrBlank(String param) {
        return param == null || param.trim().length() == 0;
    }
}
