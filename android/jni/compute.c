#include <jni.h>
#include <lcm.h>
#include <lua.h>
#include <string.h>

/**
 * State kept track of between method calls.
 */
typedef struct {
    // Lua context.
    lua_State* L;

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

static void ComputeContext_log(void* context, const lcm_LogEntry* entry);

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    return JNI_VERSION_1_6;
}

JNIEXPORT void JNICALL Java_se_ltu_emapal_compute_ComputeContext_construct(JNIEnv *env, jobject self) {
    ComputeContextState* state = calloc(1, sizeof(ComputeContextState));
    if (state == NULL) {
        (*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/OutOfMemoryError"), "Malloc failed.");
        return;
    }
    state->L = (lua_State*) luaL_newstate();
    if (state->L == NULL) {
        (*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/IllegalStateException"), "Failed to create Lua state context.");
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
    return data;
}

JNIEXPORT void JNICALL Java_se_ltu_emapal_compute_ComputeContext_registerLambda(JNIEnv *env, jobject self, jint lambdaId, jstring program) {

}

static ComputeContextState *ComputeContextState_load(JNIEnv *env, jobject self) {
    const jclass clazz = (*env)->GetObjectClass(env, self);
    const jfieldID field = (*env)->GetFieldID(env, clazz, "nativePtr", "J");
    ComputeContextState* state = (ComputeContextState*) (*env)->GetLongField(env, self, field);
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

static void ComputeContext_log(void* context, const lcm_LogEntry* entry) {
    const ComputeContextState state = *((ComputeContextState*) context);
    JNIEnv *env = state.call.env;
    jobject self = state.call.self;
    jmethodID onLogMethodID = state.call.onLogMethodID;

    jstring message = (*env)->NewStringUTF(env, entry->message.string);
    (*env)->CallVoidMethod(env, self, onLogMethodID, entry->lambda_id, entry->batch_id, message);
}