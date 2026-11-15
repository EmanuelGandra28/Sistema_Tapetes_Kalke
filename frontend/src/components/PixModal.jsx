import { useEffect, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import QRCode from 'qrcode';
import { Link } from 'react-router-dom';
import { api } from '../services/api';
import { useAuth } from '../contexts/AuthContext';
import { payloadPix } from '../utils/pix';
import { money } from '../utils/format';
import { Loading, Failure } from './State';
import Modal from './Modal';

// QR Code PIX "copia e cola" do valor da venda. Não confirma o pagamento: o vendedor confere no app do banco.
export default function PixModal({ venda, onClose }) {
  const { canAction } = useAuth();
  const q = useQuery({ queryKey: ['configuracoes', 'loja'], queryFn: () => api.get('/configuracoes/loja') });
  const loja = q.data?.data;
  const codigo = loja?.pixConfigurado ? payloadPix({ chave: loja.pixChave, nome: loja.pixNomeRecebedor, cidade: loja.pixCidade, valor: venda.total, txid: `VENDA${venda.numeroVenda}` }) : null;
  const [imagem, setImagem] = useState(null);
  const [copiado, setCopiado] = useState(false);
  useEffect(() => {
    if (!codigo) return undefined;
    let ativo = true;
    QRCode.toDataURL(codigo, { width: 260, margin: 1, errorCorrectionLevel: 'M' }).then((url) => { if (ativo) setImagem(url); });
    return () => { ativo = false; };
  }, [codigo]);
  const copiar = async () => { await window.navigator.clipboard.writeText(codigo); setCopiado(true); };
  return <Modal title={`PIX · venda #${venda.numeroVenda}`} onClose={onClose}>
    {q.isPending ? <Loading /> : q.isError ? <Failure error={q.error} /> : !codigo ? <div className="pix-box">
      <p>Os dados do PIX da loja ainda não foram cadastrados.</p>
      {canAction('configuracoes_editar') ? <Link className="button" to="/configuracoes" onClick={onClose}>Cadastrar em Configurações</Link> : <p className="hint">Peça ao dono ou ao gerente para cadastrar a chave PIX em Configurações.</p>}
    </div> : <div className="pix-box">
      <strong className="pix-valor">{money(venda.total)}</strong>
      {imagem ? <img src={imagem} alt={`QR Code PIX de ${money(venda.total)}`} width="260" height="260" /> : <Loading />}
      <p className="hint">Recebedor: {loja.pixNomeRecebedor} · {loja.pixCidade}</p>
      <button type="button" className="secondary" onClick={copiar}>{copiado ? 'Código copiado' : 'Copiar código PIX (copia e cola)'}</button>
      <p className="pix-aviso">Confira o recebimento no app do banco antes de entregar o produto.</p>
    </div>}
    <footer className="modal-footer"><button type="button" onClick={onClose}>Fechar</button></footer>
  </Modal>;
}
