package io.github.lxien.orbien.cli;

import io.github.lxien.orbien.credentials.CredentialsStore;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

@Command(
        name = "logout",
        description = "清除本地凭据"
)
public class LogoutCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        CredentialsStore.delete();
        System.out.println("凭据已清除");
        return 0;
    }
}
