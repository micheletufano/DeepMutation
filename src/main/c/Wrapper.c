#include "Wrapper.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <signal.h>
#include <sys/wait.h>

struct node_t {
    char *str;
    struct node_t *next;
};

JNIEXPORT jobjectArray JNICALL Java_edu_wm_cs_mutation_tester_Wrapper_wrapper
  (JNIEnv *env, jclass clss, jobjectArray jargs, jstring jdir, jlong jtimeout) {
    int argc = (*env)->GetArrayLength(env, jargs);
    long timeout = jtimeout;
    char buf[2048];
    int i;

    // Check arguments.
    if (argc < 2) {
        printf("usage: wrapper(abs_path, name, args...)\n");
        return NULL;
    }

    // Get directory argument
    const char *str = (*env)->GetStringUTFChars(env, jdir, 0);
    char *dir = malloc(strlen(str)+1);
    strcpy(dir, str);
    (*env)->ReleaseStringUTFChars(env, jdir, str);

    // Convert String[] to char**.
    char **args = malloc(argc * sizeof(*args));
    for (i=0; i<argc; i++) {
        jstring jstr = (jstring) (*env)->GetObjectArrayElement(env, jargs, i);
        str = (*env)->GetStringUTFChars(env, jstr, 0);
        args[i] = malloc(strlen(str)+1);
        strcpy(args[i], str);
        (*env)->ReleaseStringUTFChars(env, jstr, str);
    }

    // Save absolute path to command.
    char *abs_path = args[0];

    // Save command as list of strings.
    char **cmd = malloc((argc) * sizeof(*cmd));
    for (i=1; i<argc; i++) {
        cmd[i-1] = malloc(strlen(args[i])+1);
        strcpy(cmd[i-1], args[i]);
    }
    cmd[i-1] = NULL;

    // Pipes.
    int p[2];
    pipe(p);

    // Fork.
    pid_t pid;
    if ((pid = fork()) == 0) {
        // Child creates a new process group, changes directory, redirects output, and execs command.
        setsid();
        chdir(dir);

        close(1); dup(p[1]);            // redirect stdout to write pipe
        close(2); dup(p[1]);            // redirect stderr to write pipe
        close(0);                       // close stdin
        close(p[0]); close(p[1]);       // close pipe

        execv(abs_path, cmd);
        perror("execv");
    } else {
        // Parent waits until child finishes or timeout. Kills process group on timeout.
        close(0); dup(p[0]);            // redirect read pipe to stdin
//        close(1); close(2);             // close stdout and stderr
        close(p[0]); close(p[1]);       // close pipe

        jobjectArray ret;
        int status;
        while (waitpid(pid, &status, WNOHANG) == 0) {
            if (--timeout <= 0) {
                killpg(pid, SIGTERM);
            }
            sleep(1);
        }
        if (WIFEXITED(status)) {
            struct node_t *head = malloc(sizeof(struct node_t));
            head->str = NULL;
            head->next = NULL;
            int count = 0;

            while (fgets(buf, sizeof(buf), stdin)) {
                struct node_t *node = malloc(sizeof(struct node_t));
                node->str = malloc(strlen(buf)+1);
                strcpy(node->str, buf);
                node->next = head;
                head = node;
                count++;
            }

            ret = (jobjectArray) (*env)->NewObjectArray(env, count,
                               (*env)->FindClass(env, "java/lang/String"), (*env)->NewStringUTF(env, ""));

            struct node_t *tmp = head;
            for (i=count-1; i>=0; i--) {
                (*env)->SetObjectArrayElement(env, ret, i, (*env)->NewStringUTF(env, head->str));
                head = head->next;
                free(tmp);
                tmp = head;
            }
        } else if (WIFSIGNALED(status)) {
            ret = NULL;
        }

        for (i=0; i<argc; i++) {
            free(args[i]);
            free(cmd[i]);
        }
        free(args);
        free(cmd);

        return ret;
    }
    return NULL;
}
