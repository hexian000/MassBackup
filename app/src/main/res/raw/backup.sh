#!/system/bin/sh

[ -n "${zstd}" ] || exit 1
[ -n "${backupPath}" ] || exit 1

[ -e "${backupPath}" ] || mkdir -p "${backupPath}"
[ -e "${backupPath}" ] || exit 1

apkBkps=${backupPath}/apk
appDataBkps=${backupPath}/appdata
pkgList=/data/system/packages.list # installed packages

backup() {
  local apkFile="" pkgName=""
  mkdir -p "$apkBkps" "$appDataBkps"
  for apkFile in $(find /data/app -type f -name base.apk); do
    pkgName=$(dirname ${apkFile} | sed 's:/data/app/::; s:-.*::')
    [ "${pkgName}" = "me.hexian000.massbackup" ] && continue
    [ "$apkFile" -nt "$apkBkps/$pkgName.apk" ] &&
      (echo "$pkgName - apk";
    cp -f "$apkFile" "$apkBkps/$pkgName.apk")
    echo "$pkgName - data"
    find /data/data/${pkgName} -type f -depth -print | \
      grep -E -v '^\./cache$|^\./code_cache$|^\./app_webview/GPUCache$' | \
      cpio -o | ${zstd} >"$appDataBkps/$pkgName.cpio.zst"
  done
}

restore() {
  local Apk="" Pkg="" o=""
  for Apk in $(find ${apkBkps} -name "*.apk" -print); do
    Pkg=$(basename ${Apk} .apk)
    [ "${Pkg}" = "me.hexian000.massbackup" ] && continue
    if grep -q ${Pkg} ${pkgList}; then
      echo "$Pkg - exists"
      continue
      pm install -r "$Apk" 1> /dev/null
    else
      echo "$Pkg - apk"
      pm install "$Apk" 1> /dev/null
    fi
    if grep -q ${Pkg} ${pkgList} && [ -f "$appDataBkps/$Pkg.cpio.zst" ]; then
      echo "$Pkg - data"
      pm disable ${Pkg} 1> /dev/null
      rm -rf /data/data/${Pkg}
      ${zstd} -d <"$appDataBkps/$Pkg.cpio.zst" | cpio -idu
      o=$(grep "$Pkg" "$pkgList" | awk '{print $2}')
      chown -R ${o}:${o} /data/data/${Pkg} 2> /dev/null
      pm enable ${Pkg} 1> /dev/null
    else
      echo "Data restore failed: $Pkg" 1>&2
    fi
  done
}
