#include <android/log.h>
#include <any>
#include <hyprlang.hpp>
#include <jni.h>
#include <string>
#include <tuple>
#include <typeinfo>
#include <vector>

#define TAG "HyprlangWrapper"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

using namespace Hyprlang;

struct WrapperContext {
  std::vector<std::tuple<std::string, std::string>> keys;
  CConfig *lastConfig = nullptr;

  ~WrapperContext() {
    if (lastConfig)
      delete lastConfig;
  }
};

extern "C" JNIEXPORT jlong JNICALL
Java_com_hyprlang_parser_HyprlangParser_create(JNIEnv *env, jobject thiz) {
  LOGD("create called");
  return reinterpret_cast<jlong>(new WrapperContext());
}

extern "C" JNIEXPORT void JNICALL
Java_com_hyprlang_parser_HyprlangParser_destroy(JNIEnv *env, jobject thiz,
                                                jlong handle) {
  LOGD("destroy called");
  auto *ctx = reinterpret_cast<WrapperContext *>(handle);
  delete ctx;
}

extern "C" JNIEXPORT void JNICALL
Java_com_hyprlang_parser_HyprlangParser_addConfigValue(
    JNIEnv *env, jobject thiz, jlong handle, jstring name, jstring type,
    jobject defaultValue) {
  auto *ctx = reinterpret_cast<WrapperContext *>(handle);

  const char *nameStr = env->GetStringUTFChars(name, 0);
  const char *typeStr = env->GetStringUTFChars(type, 0);

  LOGD("addConfigValue: %s type: %s", nameStr, typeStr);

  ctx->keys.emplace_back(nameStr, typeStr);

  env->ReleaseStringUTFChars(name, nameStr);
  env->ReleaseStringUTFChars(type, typeStr);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_hyprlang_parser_HyprlangParser_parse(JNIEnv *env, jobject thiz,
                                              jlong handle, jstring input) {
  LOGD("parse called");
  auto *ctx = reinterpret_cast<WrapperContext *>(handle);
  const char *nativeInput = env->GetStringUTFChars(input, 0);

  // LOGD("Input: %s", nativeInput);

  if (ctx->lastConfig) {
    LOGD("Deleting old config");
    delete ctx->lastConfig;
    ctx->lastConfig = nullptr;
  }

  SConfigOptions options;
  options.pathIsStream = true;
  options.verifyOnly = false;
  options.allowMissingConfig = true;

  try {
    LOGD("Creating CConfig");
    ctx->lastConfig = new CConfig(nativeInput, options);

    // Register all keys
    for (const auto &[k, type] : ctx->keys) {
      LOGD("Registering key: %s", k.c_str());
      if (type == "INT")
        ctx->lastConfig->addConfigValue(k.c_str(), CConfigValue((INT)0));
      else if (type == "FLOAT")
        ctx->lastConfig->addConfigValue(k.c_str(), CConfigValue((FLOAT)0.0));
      else if (type == "STRING")
        ctx->lastConfig->addConfigValue(k.c_str(), CConfigValue((STRING) ""));
      else if (type == "VEC2")
        ctx->lastConfig->addConfigValue(k.c_str(),
                                        CConfigValue(SVector2D{0, 0}));
      else
        LOGE("Unknown type: %s", type.c_str());
    }

    LOGD("Commencing");
    ctx->lastConfig->commence();
    LOGD("Parsing");
    CParseResult result = ctx->lastConfig->parse();

    env->ReleaseStringUTFChars(input, nativeInput);

    if (result.error) {
      LOGE("Parse error: %s",
           result.getError() ? result.getError() : "Unknown");
      return env->NewStringUTF(result.getError() ? result.getError()
                                                 : "Unknown Error");
    }
    LOGD("Parse success");
    return env->NewStringUTF("");

  } catch (const std::exception &e) {
    LOGE("Exception: %s", e.what());
    env->ReleaseStringUTFChars(input, nativeInput);
    return env->NewStringUTF(e.what());
  } catch (...) {
    LOGE("Unknown Exception");
    env->ReleaseStringUTFChars(input, nativeInput);
    return env->NewStringUTF("Unknown C++ Exception");
  }
}

extern "C" JNIEXPORT jobject JNICALL
Java_com_hyprlang_parser_HyprlangParser_getConfigValue(JNIEnv *env,
                                                       jobject thiz,
                                                       jlong handle,
                                                       jstring name) {
  auto *ctx = reinterpret_cast<WrapperContext *>(handle);
  if (!ctx->lastConfig)
    return nullptr;

  const char *n = env->GetStringUTFChars(name, 0);
  // LOGD("getConfigValue: %s", n);
  auto val = ctx->lastConfig->getConfigValue(n);
  env->ReleaseStringUTFChars(name, n);

  if (!val.has_value()) {
    // LOGD("Value not found");
    return nullptr;
  }

  try {
    if (val.type() == typeid(INT)) {
      long long v = std::any_cast<INT>(val);
      jclass cls = env->FindClass("java/lang/Integer");
      jmethodID mid =
          env->GetStaticMethodID(cls, "valueOf", "(I)Ljava/lang/Integer;");
      return env->CallStaticObjectMethod(cls, mid, (int)v);
    } else if (val.type() == typeid(FLOAT)) {
      float v = std::any_cast<FLOAT>(val);
      jclass cls = env->FindClass("java/lang/Float");
      jmethodID mid =
          env->GetStaticMethodID(cls, "valueOf", "(F)Ljava/lang/Float;");
      return env->CallStaticObjectMethod(cls, mid, v);
    } else if (val.type() == typeid(STRING)) {
      try {
        const char *s = std::any_cast<STRING>(val);
        return env->NewStringUTF(s);
      } catch (...) {
        return nullptr;
      }
    }
  } catch (...) {
    LOGE("Cast exception");
    return nullptr;
  }
  return nullptr;
}
