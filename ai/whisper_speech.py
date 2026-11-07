"""Bounded cached decoding for the reviewed English Whisper ONNX exports."""

from perception_formats import pcm16


def transcribe(provider, raw, frontend):
    np = provider.np
    pcm = pcm16(raw)
    audio = np.frombuffer(pcm, dtype='<i2').astype(np.float32) / 32768
    config = provider.model_config
    hidden_size = config['d_model']
    heads = config['decoder_attention_heads']
    layers = config['decoder_layers']
    positions = config['max_source_positions']
    vocabulary_size = config['vocab_size']
    if (hidden_size, heads, layers, positions, vocabulary_size) != (768, 12, 4, 1500, 51864):
        raise ValueError('Unsupported Whisper graph dimensions')

    hidden = provider.run('whisper-encoder', {'input_features': frontend(np, audio, provider.filters)})['last_hidden_state']
    if hidden.shape != (1, positions, hidden_size):
        raise ValueError('Invalid Whisper encoder output')

    generation = provider.generation
    start = generation['decoder_start_token_id']
    end = generation['eos_token_id']
    no_timestamps = generation['no_timestamps_token_id']
    tokens = [start, no_timestamps]
    keys = {
        f'past_key_values.{layer}.{attention}.{kind}'
        for layer in range(layers)
        for attention in ('decoder', 'encoder')
        for kind in ('key', 'value')
    }
    decoder = provider.sessions['whisper-decoder']
    if {item.name for item in decoder.get_inputs() if item.name.startswith('past_key_values.')} != keys:
        raise ValueError('Unexpected Whisper cache signature')
    past = {name: np.zeros((1, heads, 0, hidden_size // heads), np.float32) for name in sorted(keys)}
    generated = []
    for step in range(provider.config['maxOutputTokens']):
        result = provider.run('whisper-decoder', {
            'input_ids': np.array([tokens], np.int64),
            'encoder_hidden_states': hidden,
            'use_cache_branch': np.array([step > 0]),
            **past,
        })
        logits = result['logits']
        if logits.shape != (1, len(tokens), vocabulary_size):
            raise ValueError('Invalid Whisper logits')
        scores = logits[0, -1].copy()
        scores[generation['suppress_tokens']] = -np.inf
        scores[no_timestamps:] = -np.inf
        if step == 0:
            scores[generation['begin_suppress_tokens']] = -np.inf
        if not np.isfinite(scores).any():
            raise ValueError('Whisper token unavailable')
        token = int(np.argmax(scores))
        if token == end:
            break
        generated.append(token)
        for name in past:
            value = result[name.replace('past_key_values.', 'present.')]
            cross_attention = '.encoder.' in name
            if cross_attention and step > 0 and not value.size:
                continue
            length = positions if cross_attention else step + 2
            if value.shape != (1, heads, length, hidden_size // heads):
                raise ValueError('Invalid Whisper cache output')
            past[name] = value
        tokens = [token]
    else:
        raise ValueError('Speech output limit reached')

    text = provider.tokenizer.decode(generated, skip_special_tokens=True).strip()
    if not text or '\0' in text or len(text.encode('utf-16-le')) // 2 > 2048:
        raise ValueError('Speech output unavailable')
    return {'text': text, 'samples': len(audio), 'sampleRate': 16000, 'language': 'en',
            'representation': provider.config['outputRepresentation']}
