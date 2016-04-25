#include <jni.h>
#include <lcm.h>
#include <lua.h>
#include <string.h>

JNIEXPORT jint JNICALL Java_se_ltu_emapal_compute_ComputeContext_errorCode(JNIEnv *env, jobject self) {
    return -1;
}

JNIEXPORT jstring JNICALL Java_se_ltu_emapal_compute_ComputeContext_errorString(JNIEnv *env, jobject self) {
    return (*env)->NewStringUTF(env, "No error!");
}

JNIEXPORT jbyteArray JNICALL Java_se_ltu_emapal_compute_ComputeContext_processBatch(JNIEnv *env, jobject self, jint lambdaId, jint batchId, jbyteArray data) {
    return data;
}

JNIEXPORT void JNICALL Java_se_ltu_emapal_compute_ComputeContext_registerLambda(JNIEnv *env, jobject self, jint lambdaId, jstring program) {

}