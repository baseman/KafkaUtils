package co.remotectrl.kafka.util;

import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import org.apache.avro.Schema;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.reflect.ReflectDatumReader;
import org.apache.kafka.common.errors.SerializationException;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Extends deserializer to support ReflectData.
 */
public class ReflectDeserializer extends KafkaAvroDeserializer {

    private DecoderFactory decoderFactory = DecoderFactory.get();

    @Override
    protected Object deserialize(
            boolean includeSchemaAndVersion,
            String topic,
            Boolean isKey,
            byte[] payload,
            Schema readerSchemaIgnored) throws SerializationException {

        if (payload == null) {
            return null;
        }

        int schemaId = -1;
        try {
            ByteBuffer buffer = ByteBuffer.wrap(payload);
            if (buffer.get() != MAGIC_BYTE) {
                throw new SerializationException("Unknown magic byte!");
            }

            schemaId = buffer.getInt();
            Schema writerSchema = schemaRegistry.getByID(schemaId);

            int start = buffer.position() + buffer.arrayOffset();
            int length = buffer.limit() - 1 - idSize;
            ReflectDatumReader<Object> reader = new ReflectDatumReader<>(writerSchema);
            BinaryDecoder decoder = decoderFactory.binaryDecoder(buffer.array(), start, length, null);
            return reader.read(null, decoder);
        } catch (IOException e) {
            throw new SerializationException("Error deserializing Avro message for id " + schemaId, e);
        } catch (RestClientException e) {
            throw new SerializationException("Error retrieving Avro schema for id " + schemaId, e);
        }
    }
}