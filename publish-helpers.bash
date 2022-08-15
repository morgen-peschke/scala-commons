export GPG_TTY=$(tty)

function __sonatype.credentials () {
    jq '"\(.sonatypeUser):\(.sonatypePassword)"' --raw-output < "$1"
}

function __sonatype.gpg () {
    jq '.gpgPassword' --raw-output < "$1"
}

function __publish.cmd () {
    local config="$1"

    if [[ -z "$config" ]]; then
	echo >&2 'Expected credentials file to be the sole argument'
	return 67
    fi
    
    if [[ $(stat -Lc "%a" "$config") != '400' ]]; then
	echo >&2 'Credentials file must be limited to 400 permissions'
	return 68
    fi
    
    printf 'mill mill.scalalib.PublishModule/publishAll __.publishArtifacts "$(__sonatype.credentials "%s")" --gpgArgs --passphrase="$(sonatype.gpg "%s")",--batch,--yes,-a,-b' "$config" "$config"
}

alias push-to-sonatype='__publish.cmd'
