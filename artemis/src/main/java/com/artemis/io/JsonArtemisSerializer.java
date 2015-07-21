package com.artemis.io;

import com.artemis.Entity;
import com.artemis.World;
import com.artemis.managers.WorldSerializationManager;
import com.artemis.utils.IntBag;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.IdentityHashMap;

public class JsonArtemisSerializer extends WorldSerializationManager.ArtemisSerializer<Json.Serializer> {
	private final Json json;
	private final ComponentLookupSerializer lookup;
	private boolean prettyPrint;

	public JsonArtemisSerializer(World world) {
		super(world);

		json = new Json(JsonWriter.OutputType.json);
		json.setIgnoreUnknownFields(true);
		lookup = new ComponentLookupSerializer(world);
		json.setSerializer(IdentityHashMap.class, lookup);
		json.setSerializer(IntBag.class, new IntBagEntitySerializer(world));
		json.setSerializer(Entity.class, new EntitySerializer(world));
	}

	public JsonArtemisSerializer prettyPrint(boolean prettyPrint) {
		this.prettyPrint = prettyPrint;
		return this;
	}

	@Override
	public JsonArtemisSerializer register(Class<?> type, Json.Serializer serializer) {
		json.setSerializer(type, serializer);
		return this;
	}

	@Override
	protected void save(Writer writer, SaveFileFormat save) {
		try {
			save.componentIdentifiers.putAll(lookup.classToIdentifierMap());
			if (prettyPrint) {
				writer.append(json.prettyPrint(save));
			} else {
				json.toJson(save, writer);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected <T extends SaveFileFormat> T load(InputStream is, Class<T> format) {
		return json.fromJson(format, is);
	}
}
