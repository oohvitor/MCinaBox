package com.aof.mcinabox.network;

import com.aof.mcinabox.network.model.AuthenticateRequest;
import com.aof.mcinabox.network.model.AuthenticateResponse;
import com.aof.mcinabox.network.model.ErrorResponse;
import com.aof.mcinabox.network.model.Profile;
import com.aof.mcinabox.network.model.RefreshRequest;
import com.aof.mcinabox.network.model.RefreshResponse;
import com.aof.mcinabox.network.model.ValidateRequest;
import com.aof.mcinabox.utils.SkinUtils;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MojangRepository {
    private static final String AUTH_BASE_URL = "https://authserver.mojang.com";
    private static final String SESSION_BASE_URL = "https://sessionserver.mojang.com";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private static MojangRepository instance = null;

    private final OkHttpClient client;
    private final Gson gson;

    private MojangRepository() {
        client = new OkHttpClient();
        gson = new Gson();
    }

    public void authenticate(AuthenticateRequest authenticateRequest, Callback<AuthenticateResponse> callback) {
        postJson(AUTH_BASE_URL + "/authenticate", authenticateRequest).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                callback.onError(null);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                try (Reader body = response.body().charStream()) {
                    if (response.code() == 200) {
                        callback.onSuccess(gson.fromJson(body, AuthenticateResponse.class));
                    } else {
                        callback.onError(gson.fromJson(body, ErrorResponse.class));
                    }
                } catch (IOException e) {
                    callback.onError(null);
                }
            }
        });
    }

    public void refresh(RefreshRequest validateRequest, Callback<RefreshResponse> callback) {
        postJson(AUTH_BASE_URL + "/refresh", validateRequest).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                callback.onError(null);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                try (Reader body = response.body().charStream()) {
                    if (response.code() == 200) {
                        callback.onSuccess(gson.fromJson(body, RefreshResponse.class));
                    } else {
                        callback.onError(gson.fromJson(body, ErrorResponse.class));
                    }
                } catch (IOException e) {
                    callback.onError(null);
                }
            }
        });
    }

    public void validate(ValidateRequest validateRequest, Callback<Void> callback) {
        postJson(AUTH_BASE_URL + "/validate", validateRequest).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                callback.onError(null);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                try (Reader body = response.body().charStream()) {
                    if (response.code() == 204) {
                        callback.onSuccess(null);
                    } else {
                        callback.onError(gson.fromJson(body, ErrorResponse.class));
                    }
                } catch (IOException e) {
                    callback.onError(null);
                }
            }
        });
    }

    public void skin(Profile profile, Callback<String> callback) {
        get(SESSION_BASE_URL + "/session/minecraft/profile/" + profile.getId()).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                callback.onError(null);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                try (Reader body = response.body().charStream()) {
                    if (response.code() == 200) {
                        callback.onSuccess(SkinUtils.getPlayerSkin(body));
                    } else {
                        callback.onError(gson.fromJson(body, ErrorResponse.class));
                    }
                } catch (IOException | JsonParseException e) {
                    callback.onError(null);
                }
            }
        });
    }

    public void head(Profile profile, String filePath, Callback<Void> callback) {
        skin(profile, new Callback<String>() {
            @Override
            public void onSuccess(String response) {
                getHead(response);
            }

            @Override
            public void onError(ErrorResponse response) {
                getHead(SkinUtils.getDefaultSkin());
            }

            private void getHead(String response) {
                get(response).enqueue(new okhttp3.Callback() {
                    @Override
                    public void onFailure(@NotNull Call call, @NotNull IOException e) {
                        callback.onError(null);
                    }

                    @Override
                    public void onResponse(@NotNull Call call, @NotNull Response response) {
                        try (InputStream is = response.body().byteStream()) {
                            if (response.code() == 200) {
                                if (SkinUtils.skinToHeadPng(is, filePath)) {
                                    callback.onSuccess(null);
                                } else {
                                    callback.onError(null);
                                }
                            } else {
                                try (InputStreamReader isr = new InputStreamReader(is)) {
                                    callback.onError(gson.fromJson(isr, ErrorResponse.class));
                                }
                            }
                        } catch (IOException e) {
                            callback.onError(null);
                        }
                    }
                });
            }
        });
    }

    private Call get(String url) {
        Request request = new Request.Builder()
                .url(url)
                .build();
        return client.newCall(request);
    }

    private Call postJson(String url, Object body) {
        RequestBody requestBody = RequestBody.create(gson.toJson(body), JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();
        return client.newCall(request);
    }

    public static MojangRepository getInstance() {
        if (instance == null) {
            synchronized (MojangRepository.class) {
                if (instance == null) {
                    instance = new MojangRepository();
                }
            }
        }
        return instance;
    }

    public interface Callback<T> {
        void onSuccess(T response);
        void onError(ErrorResponse response);
    }
}
