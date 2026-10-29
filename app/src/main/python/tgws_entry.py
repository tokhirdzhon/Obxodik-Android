import sys

from proxy.tg_ws_proxy import main


def start(host, port, secret, log_file):
    sys.argv = [
        "tgws-core",
        "--host", str(host),
        "--port", str(port),
        "--secret", str(secret),
        "--log-file", str(log_file),
        "--log-max-mb", "2",
        "--log-backups", "1",
    ]
    main()
