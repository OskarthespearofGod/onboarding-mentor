package com.onboardingmentor.ai;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.onboardingmentor.context.ClassContext;
import com.onboardingmentor.settings.MentorSettings;
import okhttp3.*;

import java.util.concurrent.TimeUnit;

public class MentorAgentClient {

    private final Project project;
    private final OkHttpClient client;
    private final Gson gson;

    public MentorAgentClient(Project project) {
        this.project = project;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .build();
        this.gson = new Gson();
    }

    public String analyzeClass(ClassContext context) throws Exception {
        String apiKey = MentorSettings.getApiKey();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new Exception("API key is empty. Please set your Anthropic API key in Settings > Tools > Onboarding Mentor");
        }

        JsonArray messages = new JsonArray();
        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.addProperty("content", "Analyze this class:\n" + context.toString());
        messages.add(userMessage);

        int iterations = 0;
        int maxIterations = 3;

        while (iterations <= maxIterations) {
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("model", "claude-haiku-4-5-20251001");
            requestBody.addProperty("max_tokens", 2048);
            requestBody.addProperty("system", "You are an onboarding mentor helping a new engineer understand a codebase.\n" +
                    "Analyze the given class. Use tools to look up related classes if needed (max 3 tool calls).\n" +
                    "Respond using standard Markdown formatting. Use exactly these headings:\n" +
                    "## What this class does\n" +
                    "## Data flow (inputs and outputs)\n" +
                    "## Key collaborators\n" +
                    "## Things to watch out for");

            requestBody.add("messages", messages);

            JsonArray tools = new JsonArray();

            JsonObject getSourceTool = new JsonObject();
            getSourceTool.addProperty("name", "get_class_source");
            getSourceTool.addProperty("description", "Looks up the class in the project and returns its source.");
            JsonObject getSourceToolInputSchema = new JsonObject();
            getSourceToolInputSchema.addProperty("type", "object");
            JsonObject getSourceToolProperties = new JsonObject();
            JsonObject classNameProp = new JsonObject();
            classNameProp.addProperty("type", "string");
            classNameProp.addProperty("description", "Fully qualified class name");
            getSourceToolProperties.add("className", classNameProp);
            getSourceToolInputSchema.add("properties", getSourceToolProperties);
            JsonArray getSourceToolRequired = new JsonArray();
            getSourceToolRequired.add("className");
            getSourceToolInputSchema.add("required", getSourceToolRequired);
            getSourceTool.add("input_schema", getSourceToolInputSchema);
            tools.add(getSourceTool);

            JsonObject getCallersTool = new JsonObject();
            getCallersTool.addProperty("name", "get_callers");
            getCallersTool.addProperty("description", "Returns up to 10 caller class names.");
            JsonObject getCallersToolInputSchema = new JsonObject();
            getCallersToolInputSchema.addProperty("type", "object");
            JsonObject getCallersToolProperties = new JsonObject();
            getCallersToolProperties.add("className", classNameProp);
            getCallersToolInputSchema.add("properties", getCallersToolProperties);
            getCallersToolInputSchema.add("required", getSourceToolRequired);
            getCallersTool.add("input_schema", getCallersToolInputSchema);
            tools.add(getCallersTool);

            requestBody.add("tools", tools);

            RequestBody body = RequestBody.create(gson.toJson(requestBody), MediaType.parse("application/json"));
            Request request = new Request.Builder()
                    .url("https://api.anthropic.com/v1/messages")
                    .addHeader("x-api-key", apiKey)
                    .addHeader("anthropic-version", "2023-06-01")
                    .post(body)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "";
                    throw new Exception("Anthropic API call failed: " + response.code() + " " + errorBody);
                }

                String responseString = response.body().string();
                JsonObject responseJson = gson.fromJson(responseString, JsonObject.class);
                String stopReason = responseJson.has("stop_reason") && !responseJson.get("stop_reason").isJsonNull()
                        ? responseJson.get("stop_reason").getAsString() : "";
                JsonArray contentArray = responseJson.getAsJsonArray("content");

                JsonObject assistantMessage = new JsonObject();
                assistantMessage.addProperty("role", "assistant");
                assistantMessage.add("content", contentArray);
                messages.add(assistantMessage);

                if ("tool_use".equals(stopReason)) {
                    JsonArray toolResults = new JsonArray();
                    for (int i = 0; i < contentArray.size(); i++) {
                        JsonObject contentItem = contentArray.get(i).getAsJsonObject();
                        if ("tool_use".equals(contentItem.get("type").getAsString())) {
                            String toolName = contentItem.get("name").getAsString();
                            String toolId = contentItem.get("id").getAsString();
                            JsonObject input = contentItem.getAsJsonObject("input");
                            String classNameArg = input.has("className") ? input.get("className").getAsString() : "";

                            String toolResultText = "Tool executed, but no content.";
                            if ("get_class_source".equals(toolName)) {
                                toolResultText = ReadAction.compute(() -> {
                                    PsiClass psiClass = JavaPsiFacade.getInstance(project).findClass(classNameArg, GlobalSearchScope.allScope(project));
                                    if (psiClass != null) {
                                        return psiClass.getText();
                                    }
                                    return "Class not found.";
                                });
                            } else if ("get_callers".equals(toolName)) {
                                toolResultText = ReadAction.compute(() -> {
                                    PsiClass psiClass = JavaPsiFacade.getInstance(project).findClass(classNameArg, GlobalSearchScope.allScope(project));
                                    if (psiClass != null) {
                                        StringBuilder callers = new StringBuilder();
                                        int[] count = {0};
                                        ReferencesSearch.search(psiClass).forEach(psiReference -> {
                                            if (count[0] >= 10) return false;
                                            PsiClass callerClass = com.intellij.psi.util.PsiTreeUtil.getParentOfType(psiReference.getElement(), PsiClass.class);
                                            if (callerClass != null && callerClass.getQualifiedName() != null) {
                                                callers.append(callerClass.getQualifiedName()).append("\n");
                                                count[0]++;
                                            }
                                            return true;
                                        });
                                        return callers.length() > 0 ? callers.toString() : "No callers found.";
                                    }
                                    return "Class not found.";
                                });
                            }

                            JsonObject toolResultContent = new JsonObject();
                            toolResultContent.addProperty("type", "text");
                            toolResultContent.addProperty("text", toolResultText);

                            JsonArray toolResultContentArray = new JsonArray();
                            toolResultContentArray.add(toolResultContent);

                            JsonObject toolResultMessage = new JsonObject();
                            toolResultMessage.addProperty("type", "tool_result");
                            toolResultMessage.addProperty("tool_use_id", toolId);
                            toolResultMessage.add("content", toolResultContentArray);

                            toolResults.add(toolResultMessage);
                        }
                    }

                    JsonObject toolResultUserMessage = new JsonObject();
                    toolResultUserMessage.addProperty("role", "user");
                    toolResultUserMessage.add("content", toolResults);
                    messages.add(toolResultUserMessage);

                    iterations++;
                } else {
                    StringBuilder finalAnswer = new StringBuilder();
                    for (int i = 0; i < contentArray.size(); i++) {
                        JsonObject contentItem = contentArray.get(i).getAsJsonObject();
                        if ("text".equals(contentItem.get("type").getAsString())) {
                            finalAnswer.append(contentItem.get("text").getAsString()).append("\n");
                        }
                    }
                    return finalAnswer.toString();
                }
            }
        }

        return "Agent stopped after maximum iterations without finalizing an answer.";
    }
}
