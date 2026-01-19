#include <android/log.h>
#include <hyprlang.hpp>
#include <jni.h>
#include <string>
#include <vector>

#define TAG "HyprlangWrapper"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

using namespace Hyprlang;

extern "C" JNIEXPORT jobject JNICALL
Java_com_hyprlang_parser_HyprlangParser_parseNative(JNIEnv *env, jobject thiz,
                                                    jstring input) {
  const char *nativeInput = env->GetStringUTFChars(input, 0);

  // Use stream mode to parse string directly
  SConfigOptions options;
  options.pathIsStream = true;
  options.verifyOnly = false;
  options.allowMissingConfig = true; // Safety

  CConfig *config = nullptr;
  std::string errorMsg = "";

  try {
    config = new CConfig(nativeInput, options);

    // Register values to avoid errors about unknown keys
    config->addConfigValue("general:border_size", CConfigValue((INT)0));
    config->addConfigValue("general:gaps_in", CConfigValue((INT)0));
    config->addConfigValue("general:gaps_out", CConfigValue((INT)0));

    config->commence();

    CParseResult result = config->parse();

    if (result.error) {
      if (result.getError()) {
        errorMsg = result.getError();
      } else {
        errorMsg = "Unknown error";
      }
      LOGE("Parse error: %s", errorMsg.c_str());
    } else {
      LOGD("Parse successful!");

      // Retrieve values to prove it works
      auto val = config->getConfigValue("general:border_size");
      if (val.has_value()) {
        try {
          // Check type before casting to avoid bad_any_cast
          if (val.type() == typeid(INT)) {
            INT borderSize = std::any_cast<INT>(val);
            LOGD("Border size: %lld", (long long)borderSize);
          } else {
            LOGE("Border size is not INT");
          }
        } catch (const std::exception &e) {
          LOGE("Failed to cast border size: %s", e.what());
        }
      }
    }
  } catch (const std::exception &e) {
    errorMsg = std::string("C++ Exception: ") + e.what();
    LOGE("%s", errorMsg.c_str());
    if (config)
      delete config;
    env->ReleaseStringUTFChars(input, nativeInput);
    return env->NewStringUTF(errorMsg.c_str());
  } catch (...) {
    errorMsg = "Unknown C++ Exception";
    LOGE("%s", errorMsg.c_str());
    if (config)
      delete config;
    env->ReleaseStringUTFChars(input, nativeInput);
    return env->NewStringUTF(errorMsg.c_str());
  }

  if (config)
    delete config;
  env->ReleaseStringUTFChars(input, nativeInput);

  return env->NewStringUTF(errorMsg.c_str());
}
