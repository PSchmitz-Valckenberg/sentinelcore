package com.sentinelcore.scoring;

public interface InstructionOverrideJudge {
    JudgeVerdict judge(String userInput, String response);
}
