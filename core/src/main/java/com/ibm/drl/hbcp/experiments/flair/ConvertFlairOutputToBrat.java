package com.ibm.drl.hbcp.experiments.flair;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ibm.drl.hbcp.extraction.extractors.flair.SentenceEntity;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

public class ConvertFlairOutputToBrat {

    public static void main(String[] args) throws Exception {
        String pathname = "../data/testfile_physicalActivity_entityPrediction/";
        String bratDir = "../brat/data/hbcp/physicalActivity/";
        File flairOutputDirectory = new File(pathname);
        if (!flairOutputDirectory.isDirectory()) {
            System.err.println(pathname + " is not a valid directory.");
            System.exit(1);
        }

        Gson gson = new Gson();

        Set<String> entitySet = new HashSet<>();

        for (File file : Objects.requireNonNull(flairOutputDirectory.listFiles())) {
            if (file.getName().endsWith("json")) {
                String shortname = file.getName().split("\\.")[0].replace(' ', '_');
                BufferedWriter bwTxt = new BufferedWriter(new FileWriter(bratDir + shortname + ".txt"));
                BufferedWriter bwAnn = new BufferedWriter(new FileWriter(bratDir + shortname + ".ann"));
                Type type = new TypeToken<List<SentenceEntity>>() {}.getType();
                InputStream inputStream = new FileInputStream(file);
                Reader reader = new BufferedReader(new InputStreamReader(inputStream));
                List<SentenceEntity> sentenceEntities = gson.fromJson(reader, type);
                int entityCount = 1;
                int charCount = 0;
                for (SentenceEntity sentenceEntity : sentenceEntities) {
                    bwTxt.write(sentenceEntity.text + "\n");
                    if (!sentenceEntity.entities.isEmpty()) {
                        for (SentenceEntity.Entity entity : sentenceEntity.entities) {
                            String bratFriendlyEntityType = entity.type.replace('.', '_').replace('(', '_').replace(')', '_');
                            entitySet.add(bratFriendlyEntityType);
                            int startPos = entity.start_pos + charCount;
                            int endPos = entity.end_pos + charCount;
                            bwAnn.write(String.format("T%d\t%s %d %d\t%s\n", entityCount, bratFriendlyEntityType, startPos, endPos, entity.text));
                            entityCount++;
                        }
                    }
                    charCount += sentenceEntity.text.length() + 1;
                }
                bwTxt.close();
                bwAnn.close();
            }
        }

        List<String> colors = Arrays.asList("red", "blue", "green", "yellow", "orange", "magenta");

        // automatically generate annotation and visual.conf file
        BufferedWriter bwAnnConf = new BufferedWriter(new FileWriter(bratDir + "annotation.conf"));
        BufferedWriter bwVisConf = new BufferedWriter(new FileWriter(bratDir + "visual.conf"));
        bwAnnConf.write("# Automatically generated visual.conf (from ConvertFlairOutputToBrat.java). This may get overwritten.\n");
        bwAnnConf.write("# This is for the entity annotation (coming from Flair) for HBCP entities.\n\n[entities]\n\n");
        bwVisConf.write("# Automatically generated visual.conf (from ConvertFlairOutputToBrat.java). This may get overwritten.\n");
        bwVisConf.write("# This is for the entity annotation (coming from Flair) for HBCP entities.\n\n[drawing]\n\n");
        ArrayList<String> entityList = new ArrayList<>(entitySet);
        for (int i = 0; i < entityList.size(); i++) {
            bwAnnConf.write(entityList.get(i) + "\n");
            bwVisConf.write(entityList.get(i) + " " + "bgColor:" + colors.get(i % colors.size()) + "\n");
        }
        bwAnnConf.write("[relations]\n\n[events]\n\n[attributes]\n\n");

        bwVisConf.write("\n\nSPAN_DEFAULT fgColor:black, bgColor:lightgreen, borderColor:darken\n\n[labels]\n\n");
        bwVisConf.close();
        bwAnnConf.close();
    }
}
