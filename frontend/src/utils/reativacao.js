// Rótulos e regras de apresentação da Reativação (a regra de negócio mora no servidor).

export const VALOR = {
  OURO: { rotulo: 'Ouro', dica: 'Entre os 20% que mais compraram' },
  PRATA: { rotulo: 'Prata', dica: 'Entre os 30% seguintes' },
  BRONZE: { rotulo: 'Bronze', dica: 'Os demais' }
};

export const CICLO = {
  EM_RISCO: { rotulo: 'Em risco', dica: 'Entre 2 e 3 meses sem comprar' },
  INATIVO: { rotulo: 'Inativo', dica: 'Entre 3 e 6 meses sem comprar' },
  INATIVO_LONGO: { rotulo: 'Inativo há muito tempo', dica: 'Mais de 6 meses sem comprar' }
};

export const STATUS_CAMPANHA = {
  RASCUNHO: { rotulo: 'Rascunho', ajuda: 'Ainda não foi aprovada: nada foi enviado.' },
  APROVADA: { rotulo: 'Em andamento', ajuda: 'Aprovada: as mensagens saem de manhã e à tarde, dentro do horário comercial.' },
  PAUSADA: { rotulo: 'Pausada', ajuda: 'Pausada: nenhuma mensagem sai até você aprovar de novo.' },
  CONCLUIDA: { rotulo: 'Concluída', ajuda: 'Todas as mensagens já foram enviadas.' },
  CANCELADA: { rotulo: 'Cancelada', ajuda: 'Cancelada: nada mais será enviado.' }
};

export const STATUS_DESTINATARIO = {
  ATIVO: 'Recebendo mensagens', CONCLUIDO: 'Recebeu as 3 mensagens', RESPONDEU: 'Respondeu (parou de receber)', SAIU: 'Pediu para sair',
  FALHOU: 'Não foi possível enviar', CANCELADO: 'Cancelado', RECOMPROU: 'Voltou a comprar'
};

export const RESPOSTA = { INTERESSADO: 'Interessado', SEM_INTERESSE: 'Sem interesse', PEDIU_SAIR: 'Pediu para sair', OUTRO: 'Outra resposta' };

// Uso da cota mensal do plano (500 mensagens): nível para colorir o contador.
export function nivelCota(enviadasNoMes, cotaMensal, bloqueioNaCota) {
  if (enviadasNoMes >= bloqueioNaCota) return 'bloqueada';
  return enviadasNoMes >= cotaMensal * 0.7 ? 'atencao' : 'ok';
}

// Taxa em porcentagem, sem dividir por zero.
export const taxa = (parte, total) => (total > 0 ? Math.round((parte / total) * 100) : 0);

// Resumo do que vai acontecer ao aprovar (mostrado na confirmação).
export function resumoAprovacao(campanha) {
  const n = campanha.destinatarios;
  return `Serão enviadas até 3 mensagens para ${n} ${n === 1 ? 'cliente' : 'clientes'}, com ${campanha.intervaloDias} dias entre elas, no máximo ${campanha.limiteDiario} por dia e só entre 8h e 20h. Quem responder para de receber.`;
}
