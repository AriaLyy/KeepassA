# coding=UTF-8
import argparse
import httplib2
import os
import sys
from googleapiclient.http import MediaFileUpload
from oauth2client.service_account import ServiceAccountCredentials
from googleapiclient import discovery

PACKAGE_NAME = 'com.lyy.keepassa'

SERVICE_ACCOUNT_EMAIL = (
    'apk-11111@playuploader.iam.gserviceaccount.com')
JSON_PATH = f'{os.path.expanduser("~")}/Documents/kpa/playuploader-db6a76306ead.json'
AUTH_API = 'https://www.googleapis.com/auth/androidpublisher'


def build_aar(cmd):
    pack_cmd = f'./gradlew -p app {cmd}'
    print(f'开始打包，打包命令：{pack_cmd}')
    os.system(pack_cmd)
    if cmd == 'bundleRelease':
        out_dir = 'bundle/devRelease'
    elif cmd == 'bundleDebug':
        out_dir = 'bundle/devDebug'
    else:
        print(f"不识别的命令: {cmd}")
        return None
    apk_dir = f'{os.getcwd()}/app/build/outputs/{out_dir}'
    print(f'dir: {apk_dir}')
    for file in os.listdir(apk_dir):
        if file.endswith('.aab'):
            aar_path = f'{apk_dir}/{file}'
        print(f'aar路径: {aar_path}')
    return aar_path


def play_share(aab_path):
    if not os.path.exists(JSON_PATH):
        print("身份文件不存在")
        return
    credentials = ServiceAccountCredentials.from_json_keyfile_name(
        JSON_PATH, AUTH_API)
    http = httplib2.Http()
    http.timeout = 10 * 60000
    http.redirect_codes = set(http.redirect_codes) - {308}
    http = credentials.authorize(http)

    service = discovery.build('androidpublisher', 'v3', http)
    media_body = MediaFileUpload(
        aab_path, mimetype='application/octet-stream', resumable=True)
    edit_request = service.internalappsharingartifacts().uploadbundle(packageName=PACKAGE_NAME,
                                                                      media_body=media_body)

    try:
        result = edit_request.execute()
        print(result)
        print(f"aab上传成功，downloadUrl: {result['downloadUrl']}")
        return True, result['downloadUrl']
    except Exception as e:
        print(f"异常，{e}")
        return False, None


def play(aab_path, track):
    credentials = ServiceAccountCredentials.from_json_keyfile_name(
        JSON_PATH, AUTH_API, AUTH_API)
    http = httplib2.Http()
    http.timeout = 10 * 60000
    http.redirect_codes = set(http.redirect_codes) - {308}
    http = credentials.authorize(http)
    service = discovery.build('androidpublisher', 'v3', http)
    edit_request = service.edits().insert(body={}, packageName=PACKAGE_NAME)
    result = edit_request.execute()
    edit_id = result['id']
    print(f'获取到edit_id: {edit_id}')
    media_body = MediaFileUpload(
        aab_path, mimetype='application/octet-stream', resumable=True)
    try:
        apk_response = service.edits().bundles().upload(
            editId=edit_id,
            packageName=PACKAGE_NAME,
            media_body=media_body).execute()
        print(f"aab上传成功，versionCode: {apk_response['versionCode']}")
        track_response = service.edits().tracks().update(
            editId=edit_id,
            track=track,
            packageName=PACKAGE_NAME,
            body={u'releases': [{
                u'name': f"{apk_response['versionCode']}版本",
                u'versionCodes': [str(apk_response['versionCode'])],
                u'status': u'completed',
            }]}).execute()
        print(
            f"轨道发布成功，track: {track}, releases: {str(track_response['releases'])}")
        commit_request = service.edits().commit(
            editId=edit_id, packageName=PACKAGE_NAME).execute()
        print(f"提交成功，edit_id: {commit_request['id']}")
        return True
    except Exception as e:
        print(f"异常，{e}")
        return False


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='kpa ci')
    # bundleRelease, bundleDebug
    parser.add_argument('-c', '--cmd', dest='cmd')
    parser.add_argument('-p', '--platform', dest='platform')  # play，play_share

    args = parser.parse_args()
    aar_path = build_aar(args.cmd)
    if not aar_path:
        print('打包失败，路径为空')
        sys.exit(-1)
    if args.platform == 'play_share':
        play_share(aar_path)
    elif args.platform == 'play':
        play(aar_path, "qa")
    sys.exit(0)
