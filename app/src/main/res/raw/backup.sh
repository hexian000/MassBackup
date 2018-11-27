#!/system/bin/sh

# Arguments:
# zstd=/path/to/zstd
# backupPath=/path/to/backups
# selfPkg=my.self.package

[ -n "${zstd}" ] || exit 1
[ -n "${backupPath}" ] || exit 1
[ -n "${selfPkg}" ] || exit 1

[ -e "${backupPath}" ] || mkdir -p "${backupPath}"
[ -d "${backupPath}" ] || exit 1

apkBkps=${backupPath}/apk
appDataBkps=${backupPath}/appdata
pkgList=/data/system/packages.list # installed packages

backup() {
  local apkFile="" pkgName=""
  mkdir -p "$apkBkps" "$appDataBkps"
  for apkFile in $(find /data/app -type f -name base.apk); do
    pkgName=$(dirname ${apkFile} | sed 's:/data/app/::; s:-.*::')
    [ "${pkgName}" = "${selfPkg}" ] && continue
    [ "$apkFile" -nt "$apkBkps/$pkgName.apk" ] &&
      (echo "$pkgName - apk"; cp -f "$apkFile" "$apkBkps/$pkgName.apk")
    echo "$pkgName - data"
    find /data/data/${pkgName} -type f -depth -print | \
      grep -E -v '^\./cache$|^\./code_cache$|^\./app_webview/GPUCache$' | \
      cpio -o | ${zstd} >"$appDataBkps/$pkgName.cpio.zst"
  done
}

restore() {
  local apkFile="" pkgName="" o=""
  for apkFile in $(find ${apkBkps} -name "*.apk" -print); do
    pkgName=$(basename ${apkFile} .apk)
    [ "${pkgName}" = "${selfPkg}" ] && continue
    if grep -q ${pkgName} ${pkgList}; then
      echo "$pkgName - exists"
      continue
      pm install -r "$apkFile" 1> /dev/null
    else
      echo "$pkgName - apk"
      pm install "$apkFile" 1> /dev/null
    fi
    if grep -q ${pkgName} ${pkgList} && [ -f "$appDataBkps/$pkgName.cpio.zst" ]; then
      echo "$pkgName - data"
      pm disable ${pkgName} 1> /dev/null
      rm -rf /data/data/${pkgName}
      ${zstd} -d <"$appDataBkps/$pkgName.cpio.zst" | cpio -idu
      o=$(grep "$pkgName" "$pkgList" | awk '{print $2}')
      chown -R ${o}:${o} /data/data/${pkgName} 2> /dev/null
      pm enable ${pkgName} 1> /dev/null
    else
      echo "Data restore failed: $pkgName" 1>&2
    fi
  done
}
