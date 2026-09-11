package com.flipkart.krystal.lattice.samples.grpc.proto2024e.sampleProtoService;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.ASSUME_DEFAULT_VALUE;
import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;
import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.WILL_NEVER_FAIL;
import static com.flipkart.krystal.model.ModelRoot.ModelType.RESPONSE;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.model.PlainJavaObject;
import com.flipkart.krystal.model.SupportedModelProtocol;
import com.flipkart.krystal.serial.DefaultSerdeProtocol;
import com.flipkart.krystal.serial.SerialId;
import com.flipkart.krystal.vajram.protobuf2024e.Protobuf2024e;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Response model for {@link Proto2024eLatticeSample} */
@ModelRoot(type = RESPONSE)
@SupportedModelProtocol(PlainJavaObject.class)
@SupportedModelProtocol(Protobuf2024e.class)
@DefaultSerdeProtocol(Protobuf2024e.class)
public interface Proto2024eLatticeSampleResponse extends Model {
  @SerialId(1)
  @IfAbsent(FAIL)
  String string();

  @SerialId(2)
  @IfAbsent(WILL_NEVER_FAIL)
  Optional<Integer> optionalInteger();

  @SerialId(3)
  @IfAbsent(WILL_NEVER_FAIL)
  @Nullable Integer nullableIntegerMayFailConditionally();

  @SerialId(4)
  @IfAbsent(WILL_NEVER_FAIL)
  @Nullable Integer nullableInteger();

  @SerialId(6)
  @IfAbsent(ASSUME_DEFAULT_VALUE)
  List<Integer> optionalIntArray();

  @SerialId(7)
  @IfAbsent(FAIL)
  int mandatoryInt();

  @SerialId(8)
  @IfAbsent(ASSUME_DEFAULT_VALUE)
  int defaultInt();

  @SerialId(9)
  @IfAbsent(FAIL)
  @Nullable String mandatoryStringPartialConstruction();

  @SerialId(10)
  @IfAbsent(ASSUME_DEFAULT_VALUE)
  @Nullable Map<String, String> mapTypedField();

  @SerialId(11)
  @IfAbsent(WILL_NEVER_FAIL)
  @Nullable ProtoMessage protoMessage();

  @SerialId(12)
  @IfAbsent(ASSUME_DEFAULT_VALUE)
  List<ProtoMessage> protoMessages();

  @SerialId(13)
  @IfAbsent(WILL_NEVER_FAIL)
  @Nullable SubMessage subMessage();

  @SerialId(14)
  @IfAbsent(ASSUME_DEFAULT_VALUE)
  List<SubMessage> subMessages();

  @SerialId(15)
  @IfAbsent(ASSUME_DEFAULT_VALUE)
  Map<String, SubMessage> namedSubMessages();

  @SerialId(16)
  @IfAbsent(FAIL)
  Status status();

  @SerialId(17)
  @IfAbsent(WILL_NEVER_FAIL)
  @Nullable Status optionalStatus();

  @SerialId(18)
  @IfAbsent(ASSUME_DEFAULT_VALUE)
  List<Status> statuses();

  @SerialId(19)
  @IfAbsent(ASSUME_DEFAULT_VALUE)
  Map<String, Status> namedStatuses();

  /**
   * Demonstrates {@link Errable} field support in proto2024e models: the inner value is stored in
   * the proto field when present (NonNil), and the proto field is absent when Nil or Failure. The
   * failure state is intentionally lost on round-trip.
   */
  @SerialId(20)
  @IfAbsent(WILL_NEVER_FAIL)
  Errable<String> errableMessage();

  // --- List<Errable<T>>: per-element Errable-wrapping; nil/failure elements are dropped on
  // write, since a protobuf repeated field has no per-element presence bit. ---
  @SerialId(21)
  @IfAbsent(ASSUME_DEFAULT_VALUE)
  List<Errable<Integer>> errableInts();

  @SerialId(22)
  @IfAbsent(ASSUME_DEFAULT_VALUE)
  List<Errable<Status>> errableStatuses();

  @SerialId(23)
  @IfAbsent(ASSUME_DEFAULT_VALUE)
  List<Errable<SubMessage>> errableSubMessages();

  // --- Errable<List<T>>: whole-field wrapping; nil/failure round-trips as an empty list wrapped
  // in Errable.withValue(...), since a repeated field can't be "absent". ---
  @SerialId(24)
  @IfAbsent(WILL_NEVER_FAIL)
  Errable<List<Integer>> errableIntList();

  @SerialId(25)
  @IfAbsent(WILL_NEVER_FAIL)
  Errable<List<Status>> errableStatusList();

  @SerialId(26)
  @IfAbsent(WILL_NEVER_FAIL)
  Errable<List<SubMessage>> errableSubMessageList();

  // --- Errable<List<Errable<T>>>: both whole-field and per-element wrapping. ---
  @SerialId(27)
  @IfAbsent(WILL_NEVER_FAIL)
  Errable<List<Errable<Integer>>> errableListOfErrableInts();

  @SerialId(28)
  @IfAbsent(WILL_NEVER_FAIL)
  Errable<List<Errable<Status>>> errableListOfErrableStatuses();

  @SerialId(29)
  @IfAbsent(WILL_NEVER_FAIL)
  Errable<List<Errable<SubMessage>>> errableListOfErrableSubMessages();

  // --- Map<K, Errable<T>>: per-element (value) Errable-wrapping; nil/failure entries are dropped
  // on write, since a protobuf map has no per-entry presence bit beyond the entry itself. ---
  @SerialId(30)
  @IfAbsent(ASSUME_DEFAULT_VALUE)
  Map<String, Errable<Integer>> errableIntMap();

  @SerialId(31)
  @IfAbsent(ASSUME_DEFAULT_VALUE)
  Map<String, Errable<Status>> errableStatusMap();

  @SerialId(32)
  @IfAbsent(ASSUME_DEFAULT_VALUE)
  Map<String, Errable<SubMessage>> errableSubMessageMap();

  // --- Errable<Map<K, T>>: whole-field wrapping; nil/failure round-trips as an empty map wrapped
  // in Errable.withValue(...). ---
  @SerialId(33)
  @IfAbsent(WILL_NEVER_FAIL)
  Errable<Map<String, Integer>> errableIntMapWhole();

  @SerialId(34)
  @IfAbsent(WILL_NEVER_FAIL)
  Errable<Map<String, Status>> errableStatusMapWhole();

  @SerialId(35)
  @IfAbsent(WILL_NEVER_FAIL)
  Errable<Map<String, SubMessage>> errableSubMessageMapWhole();

  // --- Errable<Map<K, Errable<T>>>: both whole-field and per-value wrapping. ---
  @SerialId(36)
  @IfAbsent(WILL_NEVER_FAIL)
  Errable<Map<String, Errable<Integer>>> errableMapOfErrableInts();

  @SerialId(37)
  @IfAbsent(WILL_NEVER_FAIL)
  Errable<Map<String, Errable<Status>>> errableMapOfErrableStatuses();

  @SerialId(38)
  @IfAbsent(WILL_NEVER_FAIL)
  Errable<Map<String, Errable<SubMessage>>> errableMapOfErrableSubMessages();
}
