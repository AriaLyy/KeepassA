#include <jni.h>
#include <string>

extern "C" {
#include "aes/aes.h"
}

// see https://blog.csdn.net/u010302327/article/details/79726637
//把字符串转成十六进制字符串
std::string char2hex(std::string s) {
    std::string ret;
    for (unsigned i = 0; i != s.size(); ++i) {
        char hex[5];
        sprintf(hex, "%.2x", (unsigned char) s[i]);
        ret += hex;
    }
    return ret;
}

//把十六进制字符串转成字符串
std::string hex2char(std::string s) {
    std::string ret;
    int length = (int) s.length();
    for (int i = 0; i < length; i += 2) {
        std::string buf = "0x" + s.substr(i, 2);
        unsigned int value;
        sscanf(buf.c_str(), "%x", &value);
        ret += ((char) value);
    }
    return ret;
}

int hexCharToInt(char c) {
    if (c >= '0' && c <= '9') return (c - '0');
    if (c >= 'A' && c <= 'F') return (c - 'A' + 10);
    if (c >= 'a' && c <= 'f') return (c - 'a' + 10);
    return 0;
}

//十六进制字符串转成十六进制数组
char *hexstringToBytes(std::string s) {
    int sz = (int) s.length();
    char *ret = new char[sz / 2];
    for (int i = 0; i < sz; i += 2) {
        ret[i / 2] = (char) ((hexCharToInt(s.at(i)) << 4) | hexCharToInt(s.at(i + 1)));
    }
    return ret;
}

//十六进制数组转成十六进制字符串
std::string bytestohexstring(char *bytes, int bytelength) {
    std::string str;
    std::string str2("0123456789abcdef");
    for (int i = 0; i < bytelength; ++i) {
        int b;
        b = 0x0f & (bytes[i] >> 4);
        char s1 = str2.at(b);
        str.append(1, str2.at(b));
        b = 0x0f & bytes[i];
        str.append(1, str2.at(b));
//        char s2 = str2.at(b);
    }
    return str;
}

//加密
std::string EncodeAES(const unsigned char *master_key, std::string data, const unsigned char *iv) {
    AES_KEY key;
    AES_set_encrypt_key(master_key, 128, &key);

    unsigned char ivc[AES_BLOCK_SIZE];

    std::string data_bak = data.c_str();
    unsigned int data_length = (unsigned int) data_bak.length();
    int padding = 0;
    if (data_bak.length() % AES_BLOCK_SIZE >= 0) {
        padding = (int) (AES_BLOCK_SIZE - data_bak.length() % AES_BLOCK_SIZE);
    }
    data_length += padding;
    while (padding > 0) {
        data_bak += '\0';
        padding--;
    }

    memcpy(ivc, iv, AES_BLOCK_SIZE * sizeof(char));
    std::string encryhex;
    for (unsigned int i = 0; i < data_length / AES_BLOCK_SIZE; i++) {
        std::string str16 = data_bak.substr(i * AES_BLOCK_SIZE, AES_BLOCK_SIZE);
        unsigned char out[AES_BLOCK_SIZE];
        memset(out, 0, AES_BLOCK_SIZE);
        AES_cbc_encrypt((const unsigned char *) str16.c_str(), out, 16, &key, ivc, AES_ENCRYPT);
        encryhex += bytestohexstring((char *) out, AES_BLOCK_SIZE);
    }
    return encryhex;

}

//解密
std::string DecodeAES(const unsigned char *master_key, std::string data, const unsigned char *iv) {
    AES_KEY key;
    AES_set_decrypt_key(master_key, 128, &key);

    unsigned char ivc[AES_BLOCK_SIZE];
    memcpy(ivc, iv, AES_BLOCK_SIZE * sizeof(char));
    std::string ret;
    for (unsigned int i = 0; i < data.length() / (AES_BLOCK_SIZE * 2); i++) {
        std::string str16 = data.substr(i * AES_BLOCK_SIZE * 2, AES_BLOCK_SIZE * 2);
        unsigned char out[AES_BLOCK_SIZE];
        memset(out, 0, AES_BLOCK_SIZE);
        char *buf = hexstringToBytes(str16);
        AES_cbc_encrypt((const unsigned char *) buf, out, AES_BLOCK_SIZE, &key, ivc, AES_DECRYPT);
        delete (buf);
        ret += hex2char(bytestohexstring((char *) out, AES_BLOCK_SIZE));
    }
    return ret;
}


extern
"C" JNIEXPORT jstring JNICALL
Java_com_lyy_keepassa_util_QuickUnLockUtil_encryptStr(
        JNIEnv *env,
        jclass clazz, jstring str_) {
    const char *str = env->GetStringUTFChars(str_, 0);

    const auto *master_key = (const unsigned char *) "KzSn6J0Zk4tIAQLh";
    const auto *iv = (const unsigned char *) "90abcdef12345678";

    std::string h = EncodeAES(master_key, str, iv);

    env->ReleaseStringUTFChars(str_, str);
    return env->NewStringUTF(h.c_str());
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_lyy_keepassa_util_QuickUnLockUtil_decryption(JNIEnv *env, jclass thiz, jstring str_) {
    const char *str = env->GetStringUTFChars(str_, 0);

    const auto *master_key = (const unsigned char *) "KzSn6J0Zk4tIAQLh";

    const auto *iv = (const unsigned char *) "90abcdef12345678";

    std::string s = DecodeAES(master_key, str, iv);
    env->ReleaseStringUTFChars(str_, str);

    return env->NewStringUTF(s.c_str());
}extern "C"

JNIEXPORT jstring JNICALL
Java_com_lyy_keepassa_util_QuickUnLockUtil_getDbPass(JNIEnv *env, jclass clazz) {
    std::string s = "stVz7QxFgzA7yMnH";
    return env->NewStringUTF(s.c_str());
}