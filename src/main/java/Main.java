import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;

/**
 * User: fil
 * Date: 09.09.15
 */
public class Main {

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void main(String[] args) throws IOException, InterruptedException {
        if(args == null || args.length < 3){
            System.out.println("params: {backup | restore} cassandra_data_dir keyspace_name backup_file");
            return;
        }

        String action = args[0];
        String dataDir = args[1];
        String keyspace = args[2];
        String backupFile = args[3];

        if("backup".equals(action)) {

            String backupName = "backup_" + new SimpleDateFormat("yyyyMMddHHmm").format(new Date());
            String cassandraHome = System.getenv("CASSANDRA_HOME");
            String nodeTool = (cassandraHome == null || cassandraHome.trim().isEmpty())
                    ? "nodetool" : (cassandraHome+"/bin/nodetool");

            execute(nodeTool + " clearsnapshot " + keyspace, null);
            execute(nodeTool + " snapshot -t " + backupName + " " + keyspace, null);

            Collection<File> files = FileUtils.listFiles(new File(dataDir + "/" + keyspace),
                    new IOFileFilter() {
                        @Override
                        public boolean accept(File file) {
                            return file.getPath().contains(backupName);
                        }

                        @Override
                        public boolean accept(File dir, String name) {
                            return true;
                        }
                    },
                    TrueFileFilter.TRUE);

            File backupTempDir = File.createTempFile(backupName, String.valueOf(System.currentTimeMillis()));
            backupTempDir.delete();
            backupTempDir.mkdir();

            for (File f : files) {
                String collectionName = f.getPath().substring(f.getPath().indexOf(keyspace) + keyspace.length() + 1);
                collectionName = collectionName.substring(0, collectionName.indexOf("/"));
                String fileName = f.getPath().substring(f.getPath().lastIndexOf("/"));
                FileUtils.copyFile(f, new File(backupTempDir.getPath() + "/" + keyspace + "/" + collectionName + "/" + fileName));
            }

            execute("tar -cf " + new File(backupFile)+" " + keyspace, backupTempDir);
        }else if("restore".equals(action)){
            File dataDirFile = new File(dataDir + "/" + keyspace);
            if(dataDirFile.exists()) {
                Collection<File> files = FileUtils.listFiles(dataDirFile, FileFileFilter.FILE, TrueFileFilter.TRUE);
                files.forEach(f->{
                    System.out.println("rm "+f.getPath());
                    f.delete();
                });
            }
            execute("tar -xf " + new File(backupFile), new File(dataDir));
        }else{
            System.out.println("Unexpected command action: " + action);
        }

        System.out.println("press Enter to exit...");
        System.in.read();
    }

    private static void execute(String command, File dir) throws IOException, InterruptedException {
        System.out.println("execute: "+command);
        Process p = Runtime.getRuntime().exec(command, null ,dir);
        p.waitFor();
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
        }
    }

}
