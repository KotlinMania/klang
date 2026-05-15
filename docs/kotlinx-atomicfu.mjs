import {
  createThis2j2avj17cvnv2 as createThis,
  initMetadataForClassbxx6q50dy2s7 as initMetadataForClass,
  Unit_getInstance1l3yidtaqby36 as Unit_getInstance,
  initMetadataForObject1cxne3s9w65el as initMetadataForObject,
  VOID3gxj6tk5isa35 as VOID,
  toString1pkumu07cwy4m as toString,
  toString30pk9tzaqopn as toString_0,
  equalsLong28bsrfhwvd686 as equalsLong,
  get_ONEazvfdh9ju3d4 as get_ONE,
  add85si75olwt6n as add,
  subtract16cg4lfi29fq9 as subtract,
  negate12tprdg5pyd5t as negate,
} from './kotlin-kotlin-stdlib.mjs';
//region block: imports
//endregion
//region block: pre-declaration
class atomicfu$AtomicRefArray$ref {
  static new_kotlinx_atomicfu_AtomicArray_3q052m_k$(size, $box) {
    var $this = createThis(this, $box);
    var tmp = $this;
    var tmp_0 = 0;
    // Inline function 'kotlin.arrayOfNulls' call
    var tmp_1 = Array(size);
    while (tmp_0 < size) {
      tmp_1[tmp_0] = atomic$ref$1(null);
      tmp_0 = tmp_0 + 1 | 0;
    }
    tmp.array_1 = tmp_1;
    return $this;
  }
  get_atomicfu$size_iufoqq_k$() {
    return this.array_1.length;
  }
  atomicfu$get(index) {
    return this.array_1[index];
  }
  get atomicfu$size() {
    return this.get_atomicfu$size_iufoqq_k$();
  }
}
class atomicfu$TraceBase {
  static new_kotlinx_atomicfu_TraceBase_aj12ra_k$($box) {
    return createThis(this, $box);
  }
  atomicfu$Trace$append$1(event) {
  }
  atomicfu$Trace$append$2(event1, event2) {
  }
  atomicfu$Trace$append$3(event1, event2, event3) {
  }
  atomicfu$Trace$append$4(event1, event2, event3, event4) {
  }
  invoke_wlr0vx_k$(event) {
    this.atomicfu$Trace$append$1(event());
  }
}
class None extends atomicfu$TraceBase {
  static new_kotlinx_atomicfu_TraceBase_None_hg03md_k$($box) {
    None_instance = null;
    var $this = this.new_kotlinx_atomicfu_TraceBase_aj12ra_k$($box);
    None_instance = $this;
    return $this;
  }
}
class atomicfu$TraceFormat {
  static new_kotlinx_atomicfu_TraceFormat_v139a4_k$($box) {
    return createThis(this, $box);
  }
  atomicfu$TraceFormat$format(index, event) {
    return '' + index + ': ' + toString(event);
  }
}
class AtomicRef {
  static new_kotlinx_atomicfu_AtomicRef_dkafgf_k$(value, $box) {
    var $this = createThis(this, $box);
    $this.kotlinx$atomicfu$value = value;
    return $this;
  }
  set_kotlinx$atomicfu$value_508e3y_k$(_set____db54di) {
    this.kotlinx$atomicfu$value = _set____db54di;
  }
  get_kotlinx$atomicfu$value_vi2am5_k$() {
    return this.kotlinx$atomicfu$value;
  }
  getValue_fbnwi2_k$(thisRef, property) {
    return this.kotlinx$atomicfu$value;
  }
  setValue_ttauxt_k$(thisRef, property, value) {
    this.kotlinx$atomicfu$value = value;
  }
  lazySet_57hg9d_k$(value) {
    this.kotlinx$atomicfu$value = value;
  }
  atomicfu$compareAndSet(expect, update) {
    if (!(this.kotlinx$atomicfu$value === expect))
      return false;
    this.kotlinx$atomicfu$value = update;
    return true;
  }
  atomicfu$getAndSet(value) {
    var oldValue = this.kotlinx$atomicfu$value;
    this.kotlinx$atomicfu$value = value;
    return oldValue;
  }
  toString() {
    return toString_0(this.kotlinx$atomicfu$value);
  }
}
class AtomicBoolean {
  static new_kotlinx_atomicfu_AtomicBoolean_5z2ypj_k$(value, $box) {
    var $this = createThis(this, $box);
    $this.kotlinx$atomicfu$value = value;
    return $this;
  }
  set_kotlinx$atomicfu$value_tm3k58_k$(_set____db54di) {
    this.kotlinx$atomicfu$value = _set____db54di;
  }
  get_kotlinx$atomicfu$value_vi2am5_k$() {
    return this.kotlinx$atomicfu$value;
  }
  getValue_fbnwi2_k$(thisRef, property) {
    return this.kotlinx$atomicfu$value;
  }
  setValue_bb9j9z_k$(thisRef, property, value) {
    this.kotlinx$atomicfu$value = value;
  }
  lazySet_8bd7if_k$(value) {
    this.kotlinx$atomicfu$value = value;
  }
  atomicfu$compareAndSet(expect, update) {
    if (!(this.kotlinx$atomicfu$value === expect))
      return false;
    this.kotlinx$atomicfu$value = update;
    return true;
  }
  atomicfu$getAndSet(value) {
    var oldValue = this.kotlinx$atomicfu$value;
    this.kotlinx$atomicfu$value = value;
    return oldValue;
  }
  toString() {
    return this.kotlinx$atomicfu$value.toString();
  }
}
class AtomicLong {
  static new_kotlinx_atomicfu_AtomicLong_hb423c_k$(value, $box) {
    var $this = createThis(this, $box);
    $this.kotlinx$atomicfu$value = value;
    return $this;
  }
  set_kotlinx$atomicfu$value_22wj1v_k$(_set____db54di) {
    this.kotlinx$atomicfu$value = _set____db54di;
  }
  get_kotlinx$atomicfu$value_vi2am5_k$() {
    return this.kotlinx$atomicfu$value;
  }
  getValue_fbnwi2_k$(thisRef, property) {
    return this.kotlinx$atomicfu$value;
  }
  setValue_2h12xs_k$(thisRef, property, value) {
    this.kotlinx$atomicfu$value = value;
  }
  lazySet_abm0s0_k$(value) {
    this.kotlinx$atomicfu$value = value;
  }
  atomicfu$compareAndSet(expect, update) {
    if (!equalsLong(this.kotlinx$atomicfu$value, expect))
      return false;
    this.kotlinx$atomicfu$value = update;
    return true;
  }
  atomicfu$getAndSet(value) {
    var oldValue = this.kotlinx$atomicfu$value;
    this.kotlinx$atomicfu$value = value;
    return oldValue;
  }
  atomicfu$getAndIncrement$long() {
    var _unary__edvuaz = this.kotlinx$atomicfu$value;
    this.kotlinx$atomicfu$value = add(_unary__edvuaz, get_ONE());
    return _unary__edvuaz;
  }
  atomicfu$getAndDecrement$long() {
    var _unary__edvuaz = this.kotlinx$atomicfu$value;
    this.kotlinx$atomicfu$value = subtract(_unary__edvuaz, get_ONE());
    return _unary__edvuaz;
  }
  atomicfu$getAndAdd$long(delta) {
    var oldValue = this.kotlinx$atomicfu$value;
    this.kotlinx$atomicfu$value = add(this.kotlinx$atomicfu$value, delta);
    return oldValue;
  }
  atomicfu$addAndGet$long(delta) {
    this.kotlinx$atomicfu$value = add(this.kotlinx$atomicfu$value, delta);
    return this.kotlinx$atomicfu$value;
  }
  atomicfu$incrementAndGet$long() {
    this.kotlinx$atomicfu$value = add(this.kotlinx$atomicfu$value, get_ONE());
    return this.kotlinx$atomicfu$value;
  }
  atomicfu$decrementAndGet$long() {
    this.kotlinx$atomicfu$value = subtract(this.kotlinx$atomicfu$value, get_ONE());
    return this.kotlinx$atomicfu$value;
  }
  plusAssign_p5ji1h_k$(delta) {
    this.atomicfu$getAndAdd$long(delta);
  }
  minusAssign_elja0x_k$(delta) {
    this.atomicfu$getAndAdd$long(negate(delta));
  }
  toString() {
    return this.kotlinx$atomicfu$value.toString();
  }
}
class AtomicInt {
  static new_kotlinx_atomicfu_AtomicInt_kxzl8u_k$(value, $box) {
    var $this = createThis(this, $box);
    $this.kotlinx$atomicfu$value = value;
    return $this;
  }
  set_kotlinx$atomicfu$value_nm6d3_k$(_set____db54di) {
    this.kotlinx$atomicfu$value = _set____db54di;
  }
  get_kotlinx$atomicfu$value_vi2am5_k$() {
    return this.kotlinx$atomicfu$value;
  }
  getValue_fbnwi2_k$(thisRef, property) {
    return this.kotlinx$atomicfu$value;
  }
  setValue_mm2ive_k$(thisRef, property, value) {
    this.kotlinx$atomicfu$value = value;
  }
  lazySet_u7nu62_k$(value) {
    this.kotlinx$atomicfu$value = value;
  }
  atomicfu$compareAndSet(expect, update) {
    if (!(this.kotlinx$atomicfu$value === expect))
      return false;
    this.kotlinx$atomicfu$value = update;
    return true;
  }
  atomicfu$getAndSet(value) {
    var oldValue = this.kotlinx$atomicfu$value;
    this.kotlinx$atomicfu$value = value;
    return oldValue;
  }
  atomicfu$getAndIncrement() {
    var _unary__edvuaz = this.kotlinx$atomicfu$value;
    this.kotlinx$atomicfu$value = _unary__edvuaz + 1 | 0;
    return _unary__edvuaz;
  }
  atomicfu$getAndDecrement() {
    var _unary__edvuaz = this.kotlinx$atomicfu$value;
    this.kotlinx$atomicfu$value = _unary__edvuaz - 1 | 0;
    return _unary__edvuaz;
  }
  atomicfu$getAndAdd(delta) {
    var oldValue = this.kotlinx$atomicfu$value;
    this.kotlinx$atomicfu$value = this.kotlinx$atomicfu$value + delta | 0;
    return oldValue;
  }
  atomicfu$addAndGet(delta) {
    this.kotlinx$atomicfu$value = this.kotlinx$atomicfu$value + delta | 0;
    return this.kotlinx$atomicfu$value;
  }
  atomicfu$incrementAndGet() {
    this.kotlinx$atomicfu$value = this.kotlinx$atomicfu$value + 1 | 0;
    return this.kotlinx$atomicfu$value;
  }
  atomicfu$decrementAndGet() {
    this.kotlinx$atomicfu$value = this.kotlinx$atomicfu$value - 1 | 0;
    return this.kotlinx$atomicfu$value;
  }
  plusAssign_8mmvnl_k$(delta) {
    this.atomicfu$getAndAdd(delta);
  }
  minusAssign_p980fd_k$(delta) {
    this.atomicfu$getAndAdd(-delta | 0);
  }
  toString() {
    return this.kotlinx$atomicfu$value.toString();
  }
}
class ReentrantLock {
  static new_kotlinx_atomicfu_locks_ReentrantLock_ggt17w_k$($box) {
    return createThis(this, $box);
  }
  lock_fp5s9n_k$() {
  }
  tryLock_hapj0a_k$() {
    return true;
  }
  unlock_85w96c_k$() {
  }
}
//endregion
function _get_array__jslnqg($this) {
  return $this.array_1;
}
function atomicfu$AtomicRefArray$ofNulls(size) {
  return atomicfu$AtomicRefArray$ref.new_kotlinx_atomicfu_AtomicArray_3q052m_k$(size);
}
function update(_this__u8e3s4, function_0) {
  while (true) {
    var cur = _this__u8e3s4.get_kotlinx$atomicfu$value_vi2am5_k$();
    var upd = function_0(cur);
    if (_this__u8e3s4.atomicfu$compareAndSet(cur, upd))
      return Unit_getInstance();
  }
}
function getAndUpdate(_this__u8e3s4, function_0) {
  while (true) {
    var cur = _this__u8e3s4.get_kotlinx$atomicfu$value_vi2am5_k$();
    var upd = function_0(cur);
    if (_this__u8e3s4.atomicfu$compareAndSet(cur, upd))
      return cur;
  }
}
function loop(_this__u8e3s4, action) {
  while (true) {
    action(_this__u8e3s4.get_kotlinx$atomicfu$value_vi2am5_k$());
  }
}
function loop_0(_this__u8e3s4, action) {
  while (true) {
    action(_this__u8e3s4.get_kotlinx$atomicfu$value_vi2am5_k$());
  }
}
function update_0(_this__u8e3s4, function_0) {
  while (true) {
    var cur = _this__u8e3s4.get_kotlinx$atomicfu$value_vi2am5_k$();
    var upd = function_0(cur);
    if (_this__u8e3s4.atomicfu$compareAndSet(cur, upd))
      return Unit_getInstance();
  }
}
function loop_1(_this__u8e3s4, action) {
  while (true) {
    action(_this__u8e3s4.get_kotlinx$atomicfu$value_vi2am5_k$());
  }
}
function update_1(_this__u8e3s4, function_0) {
  while (true) {
    var cur = _this__u8e3s4.get_kotlinx$atomicfu$value_vi2am5_k$();
    var upd = function_0(cur);
    if (_this__u8e3s4.atomicfu$compareAndSet(cur, upd))
      return Unit_getInstance();
  }
}
function updateAndGet(_this__u8e3s4, function_0) {
  while (true) {
    var cur = _this__u8e3s4.get_kotlinx$atomicfu$value_vi2am5_k$();
    var upd = function_0(cur);
    if (_this__u8e3s4.atomicfu$compareAndSet(cur, upd))
      return upd;
  }
}
var None_instance;
function None_getInstance() {
  if (None_instance === VOID)
    None.new_kotlinx_atomicfu_TraceBase_None_hg03md_k$();
  return None_instance;
}
function atomic$ref$1(initial) {
  return atomic$ref$(initial, None_getInstance());
}
function atomic$boolean$1(initial) {
  return atomic$boolean$(initial, None_getInstance());
}
function atomic$long$1(initial) {
  return atomic$long$(initial, None_getInstance());
}
function atomic$int$1(initial) {
  return atomic$int$(initial, None_getInstance());
}
function atomic$ref$(initial, trace) {
  return AtomicRef.new_kotlinx_atomicfu_AtomicRef_dkafgf_k$(initial);
}
function atomic$boolean$(initial, trace) {
  return AtomicBoolean.new_kotlinx_atomicfu_AtomicBoolean_5z2ypj_k$(initial);
}
function atomic$long$(initial, trace) {
  return AtomicLong.new_kotlinx_atomicfu_AtomicLong_hb423c_k$(initial);
}
function atomic$int$(initial, trace) {
  return AtomicInt.new_kotlinx_atomicfu_AtomicInt_kxzl8u_k$(initial);
}
function get_traceFormatDefault() {
  _init_properties_Trace_kt__r970dp();
  return traceFormatDefault;
}
var traceFormatDefault;
var properties_initialized_Trace_kt_s8gvpx;
function _init_properties_Trace_kt__r970dp() {
  if (!properties_initialized_Trace_kt_s8gvpx) {
    properties_initialized_Trace_kt_s8gvpx = true;
    traceFormatDefault = atomicfu$TraceFormat.new_kotlinx_atomicfu_TraceFormat_v139a4_k$();
  }
}
function get_atomicfu$reentrantLock() {
  _init_properties_Synchronized_kt__f4zdjg();
  return Lock;
}
var Lock;
var properties_initialized_Synchronized_kt_8bwsba;
function _init_properties_Synchronized_kt__f4zdjg() {
  if (!properties_initialized_Synchronized_kt_8bwsba) {
    properties_initialized_Synchronized_kt_8bwsba = true;
    Lock = ReentrantLock.new_kotlinx_atomicfu_locks_ReentrantLock_ggt17w_k$();
  }
}
//region block: post-declaration
initMetadataForClass(atomicfu$AtomicRefArray$ref, 'AtomicArray');
initMetadataForClass(atomicfu$TraceBase, 'TraceBase');
initMetadataForObject(None, 'None');
initMetadataForClass(atomicfu$TraceFormat, 'TraceFormat', atomicfu$TraceFormat.new_kotlinx_atomicfu_TraceFormat_v139a4_k$);
initMetadataForClass(AtomicRef, 'AtomicRef');
initMetadataForClass(AtomicBoolean, 'AtomicBoolean');
initMetadataForClass(AtomicLong, 'AtomicLong');
initMetadataForClass(AtomicInt, 'AtomicInt');
initMetadataForClass(ReentrantLock, 'ReentrantLock', ReentrantLock.new_kotlinx_atomicfu_locks_ReentrantLock_ggt17w_k$);
//endregion
//region block: exports
export {
  atomicfu$AtomicRefArray$ofNulls as atomicfu$AtomicRefArray$ofNulls2kz3j9ehigwa3,
  atomic$boolean$1 as atomic$boolean$1iggki4z65a2h,
  atomic$long$1 as atomic$long$129k9zwo6n9ogd,
  atomic$ref$1 as atomic$ref$130aurmcwdfdf1,
  atomic$int$1 as atomic$int$11d5swdyn6j0pu,
};
//endregion

//# sourceMappingURL=kotlinx-atomicfu.mjs.map
