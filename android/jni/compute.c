#include <jni.h>
#include <lcm.h>
#include <lua.h>
#include <stdint.h>
#include <string.h>

/**
 * State kept track of between method calls.
 */
typedef struct {
    // Lua context.
    lua_State* L;

    // The result of calling `processBatch` is stored here temporarily.
    struct {
        jbyteArray data;
    } result;

    // Call context. Is only valid for the duration of a single JNI method call.
    struct {
        // JNI environment.
        JNIEnv *env;

        // Object representing calling class instance.
        jobject self;

        // Reference to java onResult() method().
        jmethodID onResultMethodID;

        // Reference to java onLog() method.
        jmethodID onLogMethodID;
    } call;
} ComputeContextState;

static ComputeContextState* ComputeContextState_load(JNIEnv *env, jobject self);
static void ComputeContextState_save(JNIEnv *env, jobject self, const ComputeContextState *state);

static void ComputeContext_batch(void* context, const lcm_Batch* batch);
static void ComputeContext_log(void* context, const lcm_LogEntry* entry);
static void ComputeContext_throwOutOfMemoryError(JNIEnv *env, const char *message);
static void ComputeContext_throwIllegalStateException(JNIEnv *env, const char *message);

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    return JNI_VERSION_1_6;
}

JNIEXPORT void JNICALL Java_se_ltu_emapal_compute_ComputeContext_construct(JNIEnv *env, jobject self) {
    ComputeContextState* state = calloc(1, sizeof(ComputeContextState));
    if (state == NULL) {
        ComputeContext_throwOutOfMemoryError(env, "Failed to allocate ComputeContext state memory.");
        return;
    }
    state->L = (lua_State*) luaL_newstate();
    if (state->L == NULL) {
        ComputeContext_throwIllegalStateException(env, "Failed to create Lua state context.");
        return;
    }
    luaL_openlibs(state->L);
    lcm_openlib(state->L, &(lcm_Config){
       .closure_log = {
           .context = state,
           .function = ComputeContext_log,
       },
   });
   ComputeContextState_save(env, self, state);
}

JNIEXPORT void JNICALL Java_se_ltu_emapal_compute_ComputeContext_destroy(JNIEnv *env, jobject self) {
    ComputeContextState *state = ComputeContextState_load(env, self);
    if (state != NULL) {
        lua_close(state->L);
        free(state);
    }
}

JNIEXPORT jbyteArray JNICALL Java_se_ltu_emapal_compute_ComputeContext_processBatch(JNIEnv *env, jobject self, jint lambdaId, jint batchId, jbyteArray data) {
    ComputeContextState *state = ComputeContextState_load(env, self);
    int code;
    {
        jbyte* bytes = (*env)->GetByteArrayElements(env, data, NULL);
        if (bytes == NULL) {
            ComputeContext_throwIllegalStateException(env, "Failed get batch byte array elements.");
            return NULL;
        }
        const lcm_Batch input_batch = {
            .lambda_id = lambdaId,
            .batch_id = batchId,
            .data = {
                .bytes = bytes,
                .length = (*env)->GetArrayLength(env, data),
            },
        };
        code = lcm_process(state->L, input_batch, (lcm_ClosureBatch){
            .context = state,
            .function = ComputeContext_batch,
        });
        (*env)->ReleaseByteArrayElements(env, data, bytes, 0);
    }
    jstring message = (*env)->NewStringUTF(env, lcm_errstr(code));
    if (message == NULL) {
        ComputeContext_throwOutOfMemoryError(env, "Failed to allocate memory for batch result message.");
        return NULL;
    }
    (*env)->CallVoidMethod(env, self, state->call.onResultMethodID, code, message);
    jbyteArray result = state->result.data;
    state->result.data = NULL;
    return result;
}

JNIEXPORT void JNICALL Java_se_ltu_emapal_compute_ComputeContext_registerLambda(JNIEnv *env, jobject self, jint lambdaId, jstring program) {
    ComputeContextState *state = ComputeContextState_load(env, self);
    int code;
    {
        const char *lua = (*env)->GetStringUTFChars(env, program, NULL);
        if (lua == NULL) {
            ComputeContext_throwIllegalStateException(env, "Failed get program UTF-8 chars.");
            return;
        }
        code = lcm_register(state->L, (lcm_Lambda) {
            .lambda_id = lambdaId,
            .program = {
                .lua = (uint8_t*)lua,
                .length = (*env)->GetStringUTFLength(env, program),
            },
        });
        (*env)->ReleaseStringUTFChars(env, program, lua);
    }
    jstring message = (*env)->NewStringUTF(env, lcm_errstr(code));
    if (message == NULL) {
        ComputeContext_throwOutOfMemoryError(env, "Failed to allocate memory for register result message.");
        return;
    }
    (*env)->CallVoidMethod(env, self, state->call.onResultMethodID, code, message);
}

static ComputeContextState *ComputeContextState_load(JNIEnv *env, jobject self) {
    const jclass clazz = (*env)->GetObjectClass(env, self);
    const jfieldID field = (*env)->GetFieldID(env, clazz, "nativePtr", "J");
    ComputeContextState* state = (ComputeContextState*) ((*env)->GetLongField(env, self, field));
    if (state != NULL) {
        state->call.env = env;
        state->call.self = self;
        state->call.onResultMethodID = (*env)->GetMethodID(env, clazz, "onResult", "(ILjava/lang/String;)V");
        state->call.onLogMethodID = (*env)->GetMethodID(env, clazz, "onLog", "(JJLjava/lang/String;)V");
    }
    return state;
}

static void ComputeContextState_save(JNIEnv *env, jobject self, const ComputeContextState *state) {
    const jclass clazz = (*env)->GetObjectClass(env, self);
    const jfieldID field = (*env)->GetFieldID(env, clazz, "nativePtr", "J");
    (*env)->SetLongField(env, self, field, (jlong) state);
}

static void ComputeContext_batch(void* context, const lcm_Batch* batch) {
    const ComputeContextState state = *((ComputeContextState*) context);
    JNIEnv *env = state.call.env;
    jobject self = state.call.self;

    jbyteArray result = (*env)->NewByteArray(env, batch->data.length);
    if (result == NULL) {
        ComputeContext_throwOutOfMemoryError(env, "Failed to allocate memory for batch result.");
        return;
    }
    jbyte* bytes = (*env)->GetByteArrayElements(env, result, NULL);
    if (bytes == NULL) {
        ComputeContext_throwIllegalStateException(env, "Failed get batch byte array elements.");
        return;
    }
    memcpy(bytes, batch->data.bytes, batch->data.length);
    (*env)->ReleaseByteArrayElements(env, result, bytes, 0);

    ((ComputeContextState*) context)->result.data = result;
}

static void ComputeContext_log(void* context, const lcm_LogEntry* entry) {
    const ComputeContextState state = *((ComputeContextState*) context);
    JNIEnv *env = state.call.env;
    jobject self = state.call.self;
    jmethodID onLogMethodID = state.call.onLogMethodID;

    jstring message = (*env)->NewStringUTF(env, entry->message.string);
    if (message == NULL) {
        ComputeContext_throwOutOfMemoryError(env, "Failed to allocate memory for log message.");
        return;
    }
    (*env)->CallVoidMethod(env, self, onLogMethodID, entry->lambda_id, entry->batch_id, message);
}

static void ComputeContext_throwOutOfMemoryError(JNIEnv *env, const char *message) {
    (*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/OutOfMemoryError"), message);
}

static void ComputeContext_throwIllegalStateException(JNIEnv *env, const char *message) {
    (*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/IllegalStateException"), message);
}