package com.lovetropics.minigames.common.core.integration;

import com.google.common.base.Strings;
import com.google.common.net.HttpHeaders;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.lovetropics.minigames.common.config.ConfigLT;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.util.Util;
import net.minecraft.util.LenientJsonParser;
import org.slf4j.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.function.Supplier;

public interface IntegrationSender {
	Logger LOGGER = LogUtils.getLogger();

	IntegrationSender LOGGING = new Log();

	static IntegrationSender open() {
		ConfigLT.CategoryIntegrations integrations = ConfigLT.INTEGRATIONS;
		return new Http(integrations.baseUrl, integrations.authToken);
	}

	static IntegrationSender openPoll() {
		ConfigLT.CategoryIntegrations integrations = ConfigLT.INTEGRATIONS;
		return new IntegrationSender.Http(() -> "https://polling.lovetropics.com", integrations.authToken);
	}

	default boolean post(final String endpoint, final JsonElement body) {
		return post(endpoint, new Gson().toJson(body));
	}

	boolean post(final String endpoint, final String body);

	<T> Optional<T> get(String endpoint, Codec<T> codec);

	final class Http implements IntegrationSender {
		private static final HttpClient CLIENT = HttpClient.newBuilder().executor(Util.ioPool()).build();

		private final Supplier<String> url;
		private final Supplier<String> authToken;

		public Http(Supplier<String> url, Supplier<String> authToken) {
			this.url = url;
			this.authToken = authToken;
		}

		@Override
		public boolean post(String endpoint, String body) {
			if (isDisabled()) {
				return true;
			}

			try {
				LOGGER.debug("Posting {} to {}/{}", body, url.get(), endpoint);

				HttpResponse<String> response = CLIENT.send(
						request(endpoint)
								.POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
								.build(),
						HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
				);

				if (response.statusCode() >= 200 && response.statusCode() < 300) {
					LOGGER.debug("Received response from post to {}/{}: {}", url.get(), endpoint, response.body());
					return true;
				} else {
					LOGGER.error("Received unexpected response code ({}) from {}/{}: {}", response.statusCode(), url.get(), endpoint, response.body());
				}
			} catch (Exception e) {
				LOGGER.error("An exception occurred while trying to POST to {}/{}", url.get(), endpoint, e);
			}

			return false;
		}

		@Override
		public <T> Optional<T> get(String endpoint, Codec<T> codec) {
			if (isDisabled()) {
				return Optional.empty();
			}
			try {
				LOGGER.debug("Sending GET to {}/{}", url.get(), endpoint);

				HttpResponse<String> response = CLIENT.send(
						request(endpoint).GET().build(),
						HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
				);

				if (response.statusCode() >= 200 && response.statusCode() < 300) {
					LOGGER.debug("Received response from GET to {}/{}: {}", url.get(), endpoint, response.body());
					JsonElement json = LenientJsonParser.parse(response.body());
					return codec.parse(JsonOps.INSTANCE, json).resultOrPartial(error ->
							LOGGER.error("Malformed response from {}/{}: {}", url.get(), endpoint, error)
					);
				} else {
					LOGGER.error("Received unexpected response code ({}) from {}/{}: {}", response.statusCode(), url.get(), endpoint, response.body());
				}
			} catch (Exception e) {
				LOGGER.error("An exception occurred while trying to GET from {}/{}", url.get(), endpoint, e);
			}

			return Optional.empty();
		}

		private HttpRequest.Builder request(String endpoint) {
			return HttpRequest.newBuilder(URI.create(url.get() + "/" + endpoint))
					.header(HttpHeaders.USER_AGENT, "LTMinigames 1.0 (lovetropics.org)")
					.header(HttpHeaders.CONTENT_TYPE, "application/json")
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + authToken.get())
					.version(HttpClient.Version.HTTP_1_1)
					;
		}

		private boolean isDisabled() {
			return Strings.isNullOrEmpty(url.get()) || Strings.isNullOrEmpty(authToken.get());
		}
	}

	final class Log implements IntegrationSender {
		private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

		private Log() {
		}

		@Override
		public boolean post(final String endpoint, final JsonElement body) {
			return post(endpoint, GSON.toJson(body));
		}

		@Override
		public boolean post(String endpoint, String body) {
			LOGGER.info("POST to {}\n: {}", endpoint, body);
			return true;
		}

		@Override
		public <T> Optional<T> get(String endpoint, Codec<T> codec) {
			LOGGER.info("GET from {}", endpoint);
			return Optional.empty();
		}
	}
}
