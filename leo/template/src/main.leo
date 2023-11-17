program zk_ml_dna_v0.aleo {

    struct PatientAnswers {
        diseased: i8,
        age: i8,
        gender: i8
    }

    struct BiometricInput {
        // *** ML_INPUT_PLACEHOLDER ***
    }

    // research id to distributed rewards amount,
    mapping distributed_rewards: u64 => u128;

    // biometric input hash to true
    mapping submitted_biometric_inputs: field => bool;

    record RewardToken {
        owner: address,
        amount: u128
    }

    record AnswersWithModelOutput {
        owner: address,
        answers: PatientAnswers,
        model_out: i8
    }

    transition submit(answers: PatientAnswers, model_input: BiometricInput) -> (AnswersWithModelOutput, RewardToken) {
        let model_input_hash: field = BHP256::hash_to_field(model_input);

        // hardcode reward amount and researcher address for now
        let reward_amount: u128 = 1000u128;
        let researcher_address: address = aleo166zhnal73jv875wzsf6g75lutn56uj9tvxdfp3qgrawnk0nf3uzq25sqqv;

        // evaluate ML model againts provided input
        let model_out: i8 = ml(
        model_input.x0,
        model_input.x1,
        model_input.x2,
        model_input.x3,
        model_input.x4,
        model_input.x5,
        model_input.x6,
        model_input.x7,
        model_input.x8,
        model_input.x9,
        model_input.x10,
        model_input.x11,
        model_input.x12,
        model_input.x13,
        model_input.x14
        );

        return (AnswersWithModelOutput {
            owner: researcher_address, // answers and model output are private and belongs to researcher only
            answers: answers,
            model_out: model_out
        },
            RewardToken {
            owner: self.caller, // reward token goes to caller
            amount: reward_amount

        }) then finalize(model_input_hash, reward_amount);
    }

    finalize submit(model_input_hash: field, reward_amount: u128) {
        // check that it is new data, later add signature check to protect againts fraudelent inputs
        assert(!Mapping::contains(submitted_biometric_inputs, model_input_hash));

        // update total distributed rewards per research
        let research_id: u64 = 1u64;
        let current_amount: u128 = Mapping::get_or_use(distributed_rewards, research_id, 0u128);
        Mapping::set(distributed_rewards, research_id, current_amount + reward_amount);

        // publicly store hash of submitted biometric input
        Mapping::set(submitted_biometric_inputs, model_input_hash, true);
    }

    // *** ML_FUNCTION_PLACEHOLDER ***

}
