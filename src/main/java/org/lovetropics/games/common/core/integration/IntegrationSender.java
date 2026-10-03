package org.lovetropics.games.common.core.integration;

import com.google.common.base.Strings;
import com.google.common.net.HttpHeaders;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.util.LenientJsonParser;
import net.minecraft.util.Util;
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

	static IntegrationSender open(Supplier<Boolean> enabled, Supplier<String> authToken) {
		return new Http(enabled, authToken);
	}

	<T> boolean post(URI uri, Codec<T> codec, T body);

	<T> Optional<T> get(URI uri, Codec<T> codec);

	final class Http implements IntegrationSender {
		private static final HttpClient CLIENT = HttpClient.newBuilder().executor(Util.ioPool()).build();
		private static final Gson GSON = new GsonBuilder().create();

		private final Supplier<Boolean> enabled;
		private final Supplier<String> authToken;

		public Http(Supplier<Boolean> enabled, Supplier<String> authToken) {
			this.enabled = enabled;
			this.authToken = authToken;
		}

		@Override
		public <T> boolean post(URI uri, Codec<T> codec, T body) {
			if (isDisabled()) {
				return true;
			}

			try {
				JsonElement json = codec.encodeStart(JsonOps.INSTANCE, body).getOrThrow();

				LOGGER.debug("Posting {} to {}", json, uri);

				HttpResponse<String> response = CLIENT.send(
						request(uri)
								.POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(json), StandardCharsets.UTF_8))
								.build(),
						HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
				);

				if (response.statusCode() >= 200 && response.statusCode() < 300) {
					LOGGER.debug("Received response from post to {}: {}", uri, response.body());
					return true;
				} else {
					LOGGER.error("Received unexpected response code ({}) from {}: {}", response.statusCode(), uri, response.body());
				}
			} catch (Exception e) {
				LOGGER.error("An exception occurred while trying to POST {} to {}", body, uri, e);
			}

			return false;
		}

		@Override
		public <T> Optional<T> get(URI uri, Codec<T> codec) {
			if (isDisabled()) {
				return Optional.empty();
			}
			try {
				LOGGER.debug("Sending GET to {}", uri);

				HttpResponse<String> response = CLIENT.send(
						request(uri).GET().build(),
						HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
				);

				if (response.statusCode() >= 200 && response.statusCode() < 300) {
					LOGGER.debug("Received response from GET to {}: {}", uri, response.body());
					JsonElement json = LenientJsonParser.parse(response.body());
					return codec.parse(JsonOps.INSTANCE, json).resultOrPartial(error ->
							LOGGER.error("Malformed response from {}: {}", uri, error)
					);
				} else {
					LOGGER.error("Received unexpected response code ({}) from {}: {}", response.statusCode(), uri, response.body());
				}
			} catch (Exception e) {
				LOGGER.error("An exception occurred while trying to GET from {}", uri, e);
			}

			return Optional.empty();
		}

		private HttpRequest.Builder request(URI uri) {
			HttpRequest.Builder request = HttpRequest.newBuilder(uri)
					.header(HttpHeaders.USER_AGENT, "LTMinigames 1.0 (lovetropics.org)")
					.header(HttpHeaders.CONTENT_TYPE, "application/json")
					.version(HttpClient.Version.HTTP_1_1);
			String authToken = this.authToken.get();
			if (!Strings.isNullOrEmpty(authToken)) {
				request.header(HttpHeaders.AUTHORIZATION, "Bearer " + authToken);
			}
			return request;
		}

		private boolean isDisabled() {
			return !enabled.get();
		}
	}

	final class Log implements IntegrationSender {
		private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

		private Log() {
		}

		@Override
		public <T> boolean post(URI uri, Codec<T> codec, T body) {
			JsonElement json = codec.encodeStart(JsonOps.INSTANCE, body).getOrThrow();
			LOGGER.info("POST to {}\n: {}", uri, GSON.toJson(json));
			return true;
		}

		@Override
		public <T> Optional<T> get(URI uri, Codec<T> codec) {
			LOGGER.info("GET from {}", uri);
			return Optional.empty();
		}
	}
}
