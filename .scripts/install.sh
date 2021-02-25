#! /bin/bash

set -eu

app_original_executable_name=jdk-sym-link
app_executable_name=jdkslink
app_name=jdk-sym-link-cli
app_version=${1:-0.2.1}
versioned_app_name="${app_name}-${app_version}"
app_zip_file="${versioned_app_name}.zip"
download_url="https://github.com/Kevin-Lee/jdk-sym-link/releases/download/v${app_version}/${app_zip_file}"

usr_local_path="/usr/local"
opt_location="${usr_local_path}/opt"
app_location="${opt_location}/${app_name}"
installed_app_bin_path="${app_location}/bin/${app_original_executable_name}"
usr_local_bin_path="${usr_local_path}/bin"
app_bin_path="${usr_local_bin_path}/${app_executable_name}"

echo "app_executable_name=${app_executable_name}"
echo "app_name=${app_name}"
echo "app_version=${app_version}"
echo "versioned_app_name=${versioned_app_name}"
echo "app_zip_file=${app_zip_file}"
echo "download_url=${download_url}"

echo "usr_local_path=${usr_local_path}"
echo "opt_location=${opt_location}"
echo "app_location=${app_location}"
echo "installed_app_bin_path=${installed_app_bin_path}"
echo "usr_local_bin_path=${usr_local_bin_path}"
echo "app_bin_path=${app_bin_path}"

cd /tmp

curl -Lo $app_zip_file $download_url

unzip $app_zip_file || { echo "jdk-sym-link version ${app_version} doesn't seem to exist." && rm $app_zip_file && false ; }

mkdir -p $opt_location
rm -R $app_location || true
mv $versioned_app_name $app_location

echo "ln -s $installed_app_bin_path $app_bin_path"
ln -s $installed_app_bin_path $app_bin_path || true

rm $app_zip_file

echo -e "\njdk-sym-link is ready to use. Try"
echo -e "\n  ${app_executable_name} --help\n"
echo -e "\nIf it does not work, add ${usr_local_bin_path} to PATH"
echo "e.g.)"
echo -e "PATH=$usr_local_bin_path:\$PATH\n"
